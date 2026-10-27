package com.team4.rental.services.impl;

import com.team4.rental.dtos.request.AcceptRentalRequest;
import com.team4.rental.dtos.request.ReasonRequest;
import com.team4.rental.dtos.request.SendRentalRequest;
import com.team4.rental.dtos.response.ContactCardResponse;
import com.team4.rental.dtos.response.CoreLandlordContactResponse;
import com.team4.rental.dtos.response.RentalAcceptanceResponse;
import com.team4.rental.entities.ContactCard;
import com.team4.rental.entities.RentalRequest;
import com.team4.rental.entities.Tenancy;
import com.team4.rental.enums.RequestStatus;
import com.team4.rental.enums.TenancyStatus;
import com.team4.rental.events.ContactCardIssuedEvent;
import com.team4.rental.exceptions.AppException;
import com.team4.rental.exceptions.DoubleBookingException;
import com.team4.rental.exceptions.ErrorCode;
import com.team4.rental.external.CoreServiceClient;
import com.team4.rental.repositories.ActiveTenancyRepository;
import com.team4.rental.repositories.ContactCardRepository;
import com.team4.rental.repositories.RentalRequestRepository;
import com.team4.rental.repositories.TenancyRepository;
import com.team4.rental.services.RentalRequestService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalRequestServiceImpl implements RentalRequestService {
    private final RentalRequestRepository rentalRequestRepository;
    private final TenancyRepository tenancyRepository;
    private final ActiveTenancyRepository activeTenancyRepository;
    private final ContactCardRepository contactCardRepository;
    private final CoreServiceClient coreServiceClient;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public RentalRequest send(UUID tenantId, String token, SendRentalRequest request) {
        tenancyRepository.ensureRoomLock(request.getRoomId());
        tenancyRepository.lockRoom(request.getRoomId());
        tenancyRepository.expirePendingForRoom(
                request.getRoomId(),
                clock.instant(),
                TenancyStatus.PENDING,
                TenancyStatus.CANCELLED,
                "Quá hạn xác nhận tiền cọc");

        var room = coreServiceClient.getRoom(request.getRoomId(), token);
        if (!request.getRoomId().equals(room.getRoomId())
                || room.getPostId() == null
                || room.getLandlordId() == null
                || !"APPROVED".equals(room.getPostStatus())
                || !"AVAILABLE".equals(room.getRoomStatus())
                || !Boolean.FALSE.equals(room.getRented())) {
            throw new AppException(ErrorCode.ROOM_UNAVAILABLE);
        }
        if (tenantId.equals(room.getLandlordId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (tenancyRepository.existsByRoomIdAndStatusIn(
                room.getRoomId(),
                List.of(TenancyStatus.PENDING, TenancyStatus.ACTIVE))
                || activeTenancyRepository.existsByRoomId(room.getRoomId())) {
            throw new DoubleBookingException();
        }
        if (rentalRequestRepository.existsByRoomIdAndTenantIdAndStatus(
                room.getRoomId(),
                tenantId,
                RequestStatus.PENDING)) {
            throw new AppException(ErrorCode.DUPLICATE_REQUEST);
        }

        RentalRequest rentalRequest = RentalRequest.builder()
                .roomId(room.getRoomId())
                .postId(room.getPostId())
                .tenantId(tenantId)
                .landlordId(room.getLandlordId())
                .note(request.getNote())
                .status(RequestStatus.PENDING)
                .build();
        return rentalRequestRepository.save(rentalRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RentalRequest> list(
            UUID userId,
            RequestStatus status,
            UUID roomId,
            Pageable pageable) {
        return rentalRequestRepository.searchVisible(userId, status, roomId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalRequest get(UUID userId, UUID requestId) {
        return rentalRequestRepository.findVisible(requestId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    @Override
    @Transactional
    public RentalRequest cancel(UUID tenantId, UUID requestId) {
        UUID roomId = rentalRequestRepository.findTenantRoomId(requestId, tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        tenancyRepository.ensureRoomLock(roomId);
        tenancyRepository.lockRoom(roomId);

        RentalRequest rentalRequest = rentalRequestRepository.lockTenantRequest(requestId, tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (rentalRequest.getStatus() == RequestStatus.CANCELLED) {
            return rentalRequest;
        }
        if (rentalRequest.getStatus() != RequestStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_STATE);
        }

        rentalRequest.setStatus(RequestStatus.CANCELLED);
        return rentalRequestRepository.save(rentalRequest);
    }

    @Override
    @Transactional
    public RentalAcceptanceResponse accept(
            UUID landlordId,
            String token,
            UUID requestId,
            AcceptRentalRequest request) {
        UUID roomId = rentalRequestRepository.findOwnedRoomId(requestId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        tenancyRepository.ensureRoomLock(roomId);
        tenancyRepository.lockRoom(roomId);
        tenancyRepository.expirePendingForRoom(
                roomId,
                clock.instant(),
                TenancyStatus.PENDING,
                TenancyStatus.CANCELLED,
                "Quá hạn xác nhận tiền cọc");

        RentalRequest rentalRequest = rentalRequestRepository.lockOwned(requestId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (rentalRequest.getStatus() == RequestStatus.ACCEPTED) {
            Tenancy tenancy = tenancyRepository.findByRentalRequestId(requestId)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_STATE));
            ContactCard contactCard = contactCardRepository.findByRentalRequestId(requestId)
                    .orElseThrow(() -> new AppException(ErrorCode.CONTACT_CARD_NOT_FOUND));

            ContactCardResponse contactCardResponse = ContactCardResponse.builder()
                    .id(contactCard.getId())
                    .rentalRequestId(contactCard.getRentalRequestId())
                    .landlordId(contactCard.getLandlordId())
                    .landlordName(contactCard.getLandlordName())
                    .landlordPhone(contactCard.getLandlordPhone())
                    .landlordZalo(contactCard.getLandlordZalo())
                    .landlordAvatarUrl(contactCard.getLandlordAvatarUrl())
                    .note(contactCard.getNote())
                    .issuedAt(contactCard.getIssuedAt())
                    .build();

            return RentalAcceptanceResponse.builder()
                    .tenancy(tenancy)
                    .contactCard(contactCardResponse)
                    .build();
        }
        if (rentalRequest.getStatus() != RequestStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_STATE);
        }

        var room = coreServiceClient.getRoom(roomId, token);
        if (!landlordId.equals(room.getLandlordId())
                || !roomId.equals(room.getRoomId())
                || !rentalRequest.getPostId().equals(room.getPostId())
                || !"APPROVED".equals(room.getPostStatus())
                || !"AVAILABLE".equals(room.getRoomStatus())
                || !Boolean.FALSE.equals(room.getRented())) {
            throw new AppException(ErrorCode.ROOM_UNAVAILABLE);
        }

        CoreLandlordContactResponse landlord = coreServiceClient.getLandlordContact(landlordId, token);
        if (landlord.getPhoneNumber() == null
                || landlord.getPhoneNumber().isBlank()
                || landlord.getZalo() == null
                || landlord.getZalo().isBlank()) {
            throw new AppException(ErrorCode.LANDLORD_CONTACT_INCOMPLETE);
        }

        Instant deadline = request.getDepositDeadline() == null
                ? clock.instant().plus(Duration.ofHours(24))
                : request.getDepositDeadline();
        if (!deadline.isAfter(clock.instant())
                || deadline.atZone(clock.getZone()).toLocalDate().isAfter(request.getEndDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getStartDate().isBefore(LocalDate.now(clock))
                || !request.getEndDate().isAfter(request.getStartDate())
                || room.getAvailableFrom() != null
                && request.getStartDate().isBefore(room.getAvailableFrom())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (tenancyRepository.existsByRoomIdAndStatusIn(
                roomId,
                List.of(TenancyStatus.PENDING, TenancyStatus.ACTIVE))
                || activeTenancyRepository.existsByRoomId(roomId)) {
            throw new DoubleBookingException();
        }

        Tenancy tenancy = tenancyRepository.saveAndFlush(Tenancy.builder()
                .contractCode("HD-" + UUID.randomUUID())
                .rentalRequestId(requestId)
                .roomId(roomId)
                .landlordId(landlordId)
                .tenantId(rentalRequest.getTenantId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .depositDeadline(deadline)
                .monthlyRent(request.getMonthlyRent())
                .depositAmount(request.getDepositAmount())
                .electricityUnitPrice(request.getElectricityUnitPrice())
                .waterUnitPrice(request.getWaterUnitPrice())
                .initialElectricity(request.getInitialElectricity())
                .initialWater(request.getInitialWater())
                .termsAndConditions(request.getTermsAndConditions())
                .status(TenancyStatus.PENDING)
                .build());

        rentalRequest.setStatus(RequestStatus.ACCEPTED);
        rentalRequestRepository.saveAndFlush(rentalRequest);
        rentalRequestRepository.rejectOtherPending(
                roomId,
                requestId,
                RequestStatus.PENDING,
                RequestStatus.REJECTED,
                "Phòng đã được chấp nhận cho một yêu cầu thuê khác",
                clock.instant());

        ContactCard contactCard = contactCardRepository.saveAndFlush(ContactCard.builder()
                .rentalRequestId(requestId)
                .landlordId(landlordId)
                .landlordName(landlord.getFullName())
                .landlordPhone(landlord.getPhoneNumber())
                .landlordZalo(landlord.getZalo())
                .landlordAvatarUrl(landlord.getAvatarUrl())
                .note(request.getContactNote() == null || request.getContactNote().isBlank()
                        ? null
                        : request.getContactNote().strip())
                .build());

        eventPublisher.publishEvent(ContactCardIssuedEvent.builder()
                .tenantId(rentalRequest.getTenantId())
                .rentalRequestId(requestId)
                .accessToken(token)
                .build());

        ContactCardResponse contactCardResponse = ContactCardResponse.builder()
                .id(contactCard.getId())
                .rentalRequestId(contactCard.getRentalRequestId())
                .landlordId(contactCard.getLandlordId())
                .landlordName(contactCard.getLandlordName())
                .landlordPhone(contactCard.getLandlordPhone())
                .landlordZalo(contactCard.getLandlordZalo())
                .landlordAvatarUrl(contactCard.getLandlordAvatarUrl())
                .note(contactCard.getNote())
                .issuedAt(contactCard.getIssuedAt())
                .build();

        return RentalAcceptanceResponse.builder()
                .tenancy(tenancy)
                .contactCard(contactCardResponse)
                .build();
    }

    @Override
    @Transactional
    public RentalRequest reject(UUID landlordId, UUID requestId, ReasonRequest request) {
        UUID roomId = rentalRequestRepository.findOwnedRoomId(requestId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        tenancyRepository.ensureRoomLock(roomId);
        tenancyRepository.lockRoom(roomId);

        RentalRequest rentalRequest = rentalRequestRepository.lockOwned(requestId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (rentalRequest.getStatus() != RequestStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_STATE);
        }

        rentalRequest.setStatus(RequestStatus.REJECTED);
        rentalRequest.setRejectReason(request.getReason().strip());
        return rentalRequestRepository.save(rentalRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ContactCardResponse getContactCard(UUID userId, UUID requestId) {
        RentalRequest rentalRequest = rentalRequestRepository.findVisible(requestId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (rentalRequest.getStatus() != RequestStatus.ACCEPTED) {
            throw new AppException(ErrorCode.CONTACT_CARD_NOT_FOUND);
        }

        ContactCard contactCard = contactCardRepository.findByRentalRequestId(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTACT_CARD_NOT_FOUND));

        return ContactCardResponse.builder()
                .id(contactCard.getId())
                .rentalRequestId(contactCard.getRentalRequestId())
                .landlordId(contactCard.getLandlordId())
                .landlordName(contactCard.getLandlordName())
                .landlordPhone(contactCard.getLandlordPhone())
                .landlordZalo(contactCard.getLandlordZalo())
                .landlordAvatarUrl(contactCard.getLandlordAvatarUrl())
                .note(contactCard.getNote())
                .issuedAt(contactCard.getIssuedAt())
                .build();
    }
}
