package com.team4.rental.services.impl;

import com.team4.rental.dtos.request.*;
import com.team4.rental.entities.*;
import com.team4.rental.enums.*;
import com.team4.rental.exceptions.*;
import com.team4.rental.external.CoreServiceClient;
import com.team4.rental.repositories.*;
import com.team4.rental.services.RentalRequestService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalRequestServiceImpl implements RentalRequestService {
    private final RentalRequestRepository rentalRequests;
    private final TenancyRepository tenancies;
    private final ActiveTenancyRepository activeTenancies;
    private final CoreServiceClient coreServiceClient;

    @Override
    @Transactional
    public RentalRequest send(UUID tenantId, String token, SendRentalRequest request) {
        // Tuần tự hóa cả các yêu cầu mới lẫn việc chấp thuận, bao gồm cả trường hợp
        // chưa có hợp đồng.
        tenancies.ensureRoomLock(request.getRoomId());
        tenancies.lockRoom(request.getRoomId());
        var room = coreServiceClient.getRoom(request.getRoomId(), token);
        if (!request.getRoomId().equals(room.getRoomId()) || room.getPostId() == null
                || room.getLandlordId() == null || !"APPROVED".equals(room.getPostStatus())
                || !"AVAILABLE".equals(room.getRoomStatus()) || !Boolean.FALSE.equals(room.getRented())) {
            throw new AppException(ErrorCode.ROOM_UNAVAILABLE);
        }
        if (tenantId.equals(room.getLandlordId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (tenancies.existsByRoomIdAndStatusIn(room.getRoomId(),
                List.of(TenancyStatus.PENDING, TenancyStatus.ACTIVE))
                || activeTenancies.existsByRoomId(room.getRoomId())) {
            throw new DoubleBookingException();
        }
        if (rentalRequests.existsByRoomIdAndTenantIdAndStatus(
                room.getRoomId(), tenantId, RequestStatus.PENDING)) {
            throw new AppException(ErrorCode.DUPLICATE_REQUEST);
        }
        return rentalRequests.save(RentalRequest.builder()
                .roomId(room.getRoomId()).postId(room.getPostId()).tenantId(tenantId)
                .landlordId(room.getLandlordId()).note(request.getNote())
                .status(RequestStatus.PENDING).build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RentalRequest> list(UUID userId, String role, Pageable pageable) {
        return "LANDLORD".equals(role)
                ? rentalRequests.findByLandlordId(userId, pageable)
                : rentalRequests.findByTenantId(userId, pageable);
    }

    @Override
    @Transactional
    public Tenancy accept(UUID landlordId, String token, UUID requestId, AcceptRentalRequest request) {
        UUID roomId = rentalRequests.findOwnedRoomId(requestId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        tenancies.ensureRoomLock(roomId);
        tenancies.lockRoom(roomId);
        var rentalRequest = rentalRequests.lockOwned(requestId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (rentalRequest.getStatus() == RequestStatus.ACCEPTED) {
            return tenancies.findByRentalRequestId(requestId)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_STATE));
        }
        if (rentalRequest.getStatus() != RequestStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_STATE);
        }
        var room = coreServiceClient.getRoom(roomId, token);
        if (!landlordId.equals(room.getLandlordId()) || !roomId.equals(room.getRoomId())
                || !rentalRequest.getPostId().equals(room.getPostId())
                || !"APPROVED".equals(room.getPostStatus())
                || !"AVAILABLE".equals(room.getRoomStatus()) || !Boolean.FALSE.equals(room.getRented())) {
            throw new AppException(ErrorCode.ROOM_UNAVAILABLE);
        }
        if (request.getStartDate().isBefore(LocalDate.now())
                || !request.getEndDate().isAfter(request.getStartDate())
                || (room.getAvailableFrom() != null && request.getStartDate().isBefore(room.getAvailableFrom()))) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (tenancies.existsByRoomIdAndStatusIn(roomId, List.of(TenancyStatus.PENDING, TenancyStatus.ACTIVE))
                || activeTenancies.existsByRoomId(roomId)) {
            throw new DoubleBookingException();
        }
        var tenancy = tenancies.saveAndFlush(Tenancy.builder()
                .contractCode("HD-" + UUID.randomUUID()).rentalRequestId(requestId)
                .roomId(roomId).landlordId(landlordId).tenantId(rentalRequest.getTenantId())
                .startDate(request.getStartDate()).endDate(request.getEndDate())
                .monthlyRent(request.getMonthlyRent()).depositAmount(request.getDepositAmount())
                .electricityUnitPrice(request.getElectricityUnitPrice())
                .waterUnitPrice(request.getWaterUnitPrice())
                .initialElectricity(request.getInitialElectricity()).initialWater(request.getInitialWater())
                .termsAndConditions(request.getTermsAndConditions()).status(TenancyStatus.PENDING).build());
        rentalRequest.setStatus(RequestStatus.ACCEPTED);
        rentalRequests.saveAndFlush(rentalRequest);
        rentalRequests.rejectOtherPending(roomId, requestId, RequestStatus.PENDING,
                RequestStatus.REJECTED, "Phòng đã được chấp nhận cho một yêu cầu thuê khác", Instant.now());
        return tenancy;
    }

    @Override
    @Transactional
    public RentalRequest reject(UUID landlordId, UUID requestId, ReasonRequest request) {
        UUID roomId = rentalRequests.findOwnedRoomId(requestId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        tenancies.ensureRoomLock(roomId);
        tenancies.lockRoom(roomId);
        var rentalRequest = rentalRequests.lockOwned(requestId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (rentalRequest.getStatus() != RequestStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_STATE);
        }
        rentalRequest.setStatus(RequestStatus.REJECTED);
        rentalRequest.setRejectReason(request.getReason().strip());
        return rentalRequests.save(rentalRequest);
    }
}
