package com.team4.core.repositories;

import com.team4.core.entities.Room;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    Optional<Room> findByIdAndFloorHouseLandlordId(UUID id, UUID landlordId);
}
