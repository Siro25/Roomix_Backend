package com.team4.core.repositories;

import com.team4.core.entities.Room;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, UUID>, JpaSpecificationExecutor<Room> {
    Optional<Room> findByIdAndFloor_House_Landlord_Id(UUID id, UUID landlordId);
    boolean existsByFloor_IdAndRoomNumber(UUID floorId, String roomNumber);
    boolean existsByFloor_IdAndRoomNumberAndIdNot(UUID floorId, String roomNumber, UUID id);

    @Query("select r.floor.house.id from Room r where r.id = :id and r.floor.house.landlord.id = :landlordId")
    Optional<UUID> findOwnedHouseId(@Param("id") UUID id, @Param("landlordId") UUID landlordId);
}
