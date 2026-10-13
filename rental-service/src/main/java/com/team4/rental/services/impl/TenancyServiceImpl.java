package com.team4.rental.services.impl;

import com.team4.rental.dtos.request.*;
import com.team4.rental.entities.*;
import com.team4.rental.enums.*;
import com.team4.rental.exceptions.*;
import com.team4.rental.repositories.*;
import com.team4.rental.services.TenancyService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenancyServiceImpl implements TenancyService {
    private final TenancyRepository tenancies;
    private final ActiveTenancyRepository activeTenancies;
    private final TenancyEventRepository events;

    @Override
    @Transactional(readOnly = true)
    public Page<Tenancy> list(UUID userId, String role, Pageable pageable) {
        return "LANDLORD".equals(role) ? tenancies.findByLandlordId(userId, pageable)
                : tenancies.findByTenantId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Tenancy get(UUID userId, UUID tenancyId) {
        return tenancies.findVisible(tenancyId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    @Override
    @Transactional
    public Tenancy confirmDeposit(UUID landlordId, UUID tenancyId) {
        UUID roomId = tenancies.findVisibleRoomId(tenancyId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        tenancies.ensureRoomLock(roomId);
        tenancies.lockRoom(roomId);
        var tenancy = tenancies.lockOwned(tenancyId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (tenancy.getStatus() == TenancyStatus.ACTIVE) {
            return tenancy;
        }
        if (tenancy.getStatus() != TenancyStatus.PENDING || tenancy.getEndDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_STATE);
        }
        if (activeTenancies.existsByRoomId(roomId)) {
            throw new DoubleBookingException();
        }
        activeTenancies.saveAndFlush(ActiveTenancy.builder().roomId(roomId)
                .tenancyId(tenancyId).tenantId(tenancy.getTenantId())
                .checkInDate(tenancy.getStartDate()).build());
        tenancy.setStatus(TenancyStatus.ACTIVE);
        tenancy.setDepositConfirmedAt(Instant.now());
        tenancies.save(tenancy);
        // Persist in the same transaction. Kafka delivery is intentionally deferred.
        events.save(TenancyEvent.builder().eventId(UUID.randomUUID())
                .eventType(TenancyEventType.TENANCY_ACTIVATED).roomId(roomId).tenancyId(tenancyId)
                .partitionKey(roomId.toString()).topic("rental-events-topic")
                .occurredAt(Instant.now()).build());
        return tenancy;
    }

    @Override
    @Transactional
    public Tenancy cancel(UUID userId, UUID tenancyId, ReasonRequest request) {
        UUID roomId = tenancies.findVisibleRoomId(tenancyId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        tenancies.ensureRoomLock(roomId);
        tenancies.lockRoom(roomId);
        var tenancy = tenancies.lockVisible(tenancyId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (tenancy.getStatus() == TenancyStatus.CANCELLED) {
            return tenancy;
        }
        if (tenancy.getStatus() != TenancyStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_STATE);
        }
        tenancy.setStatus(TenancyStatus.CANCELLED);
        tenancy.setCancellationReason(request.getReason().strip());
        return tenancies.save(tenancy);
    }

    @Override
    @Transactional
    public Tenancy end(UUID landlordId, UUID tenancyId, EndTenancyRequest request) {
        UUID roomId = tenancies.findVisibleRoomId(tenancyId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        tenancies.ensureRoomLock(roomId);
        tenancies.lockRoom(roomId);
        var tenancy = tenancies.lockOwned(tenancyId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (tenancy.getStatus() == TenancyStatus.ENDED) {
            return tenancy;
        }
        if (tenancy.getStatus() != TenancyStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_STATE);
        }
        if (request.getActualReturnDate().isBefore(tenancy.getStartDate())
                || request.getActualReturnDate().isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        tenancy.setStatus(TenancyStatus.ENDED);
        tenancy.setActualReturnDate(request.getActualReturnDate());
        tenancies.save(tenancy);
        activeTenancies.deleteByTenancyId(tenancyId);
        events.save(TenancyEvent.builder().eventId(UUID.randomUUID())
                .eventType(TenancyEventType.TENANCY_ENDED).roomId(roomId).tenancyId(tenancyId)
                .partitionKey(roomId.toString()).topic("rental-events-topic")
                .occurredAt(Instant.now()).build());
        return tenancy;
    }
}
