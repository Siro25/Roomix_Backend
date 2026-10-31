package com.team4.core.repositories;

import com.team4.core.entities.Room;
import com.team4.core.enums.RoomStatus;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    Optional<Room> findByIdAndFloorHouseLandlordId(UUID id, UUID landlordId);

    boolean existsByFloorId(UUID floorId);

    boolean existsByFloorHouseId(UUID houseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from Room room where room.id = :roomId")
    Optional<Room> lockById(@Param("roomId") UUID roomId);

    long countByStatus(RoomStatus status);

    long countByStatusAndRentedFalse(RoomStatus status);

    long countByRentedTrue();

    long countByFloorHouseLandlordId(UUID landlordId);

    long countByFloorHouseLandlordIdAndStatus(UUID landlordId, RoomStatus status);

    long countByFloorHouseLandlordIdAndStatusAndRentedFalse(
            UUID landlordId,
            RoomStatus status);

    long countByFloorHouseLandlordIdAndRentedTrue(UUID landlordId);
}
