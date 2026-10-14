package com.team4.rental.repositories;

import com.team4.rental.entities.RentalRequest;
import com.team4.rental.enums.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface RentalRequestRepository extends JpaRepository<RentalRequest, UUID> {
    @Query("""
            select r from RentalRequest r
            where (r.tenantId = :userId or r.landlordId = :userId)
              and (:status is null or r.status = :status)
              and (:roomId is null or r.roomId = :roomId)
            """)
    Page<RentalRequest> searchVisible(UUID userId, RequestStatus status, UUID roomId, Pageable pageable);

    @Query("select r from RentalRequest r where r.id = :id and (r.tenantId = :userId or r.landlordId = :userId)")
    Optional<RentalRequest> findVisible(UUID id, UUID userId);

    @Query("select r.roomId from RentalRequest r where r.id = :id and r.tenantId = :tenantId")
    Optional<UUID> findTenantRoomId(UUID id, UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RentalRequest r where r.id = :id and r.tenantId = :tenantId")
    Optional<RentalRequest> lockTenantRequest(UUID id, UUID tenantId);

    Page<RentalRequest> findByTenantId(UUID tenantId, Pageable pageable);
    Page<RentalRequest> findByLandlordId(UUID landlordId, Pageable pageable);
    Optional<RentalRequest> findByIdAndLandlordId(UUID id, UUID landlordId);
    @Query("select r.roomId from RentalRequest r where r.id = :id and r.landlordId = :landlordId")
    Optional<UUID> findOwnedRoomId(UUID id, UUID landlordId);
    boolean existsByRoomIdAndTenantIdAndStatus(UUID roomId, UUID tenantId, RequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RentalRequest r where r.id = :id and r.landlordId = :landlordId")
    Optional<RentalRequest> lockOwned(UUID id, UUID landlordId);

    @Modifying
    @Query("""
            update RentalRequest r set r.status = :rejected, r.rejectReason = :reason,
                r.updatedAt = :now
            where r.roomId = :roomId and r.id <> :acceptedId and r.status = :pending
            """)
    int rejectOtherPending(UUID roomId, UUID acceptedId, RequestStatus pending,
                           RequestStatus rejected, String reason, Instant now);
}
