package com.team4.core.repositories;

import com.team4.core.entities.Floor;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorRepository extends JpaRepository<Floor, UUID> {
    Optional<Floor> findByIdAndHouseIdAndHouseLandlordId(UUID id, UUID houseId, UUID landlordId);
}
