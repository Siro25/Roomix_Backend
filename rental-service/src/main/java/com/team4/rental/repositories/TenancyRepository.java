package com.team4.rental.repositories;

import com.team4.rental.entities.Tenancy;
import com.team4.rental.enums.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface TenancyRepository extends JpaRepository<Tenancy, UUID> {
    boolean existsByRoomIdAndStatusIn(UUID roomId, Collection<TenancyStatus> statuses);
    Optional<Tenancy> findByRentalRequestId(UUID rentalRequestId);
    Optional<Tenancy> findByIdAndLandlordId(UUID id, UUID landlordId);
    @Query("select t.roomId from Tenancy t where t.id = :id and (t.landlordId = :userId or t.tenantId = :userId)")
    Optional<UUID> findVisibleRoomId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tenancy t where t.id = :id and (t.landlordId = :userId or t.tenantId = :userId)")
    Optional<Tenancy> lockVisible(UUID id, UUID userId);
    Page<Tenancy> findByLandlordId(UUID landlordId, Pageable pageable);
    Page<Tenancy> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("select t from Tenancy t where t.id = :id and (t.tenantId = :userId or t.landlordId = :userId)")
    Optional<Tenancy> findVisible(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tenancy t where t.id = :id and t.landlordId = :landlordId")
    Optional<Tenancy> lockOwned(UUID id, UUID landlordId);

    @Modifying
    @Query(value = "insert into room_booking_locks(room_id) values (:roomId) on conflict do nothing",
           nativeQuery = true)
    void ensureRoomLock(UUID roomId);

    @Query(value = "select room_id from room_booking_locks where room_id = :roomId for update",
           nativeQuery = true)
    UUID lockRoom(UUID roomId);
}
