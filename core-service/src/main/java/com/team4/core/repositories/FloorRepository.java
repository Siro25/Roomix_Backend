package com.team4.core.repositories;

import com.team4.core.entities.Floor;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorRepository extends JpaRepository<Floor, UUID> {
    Optional<Floor> findByIdAndHouseIdAndHouseLandlordId(UUID id, UUID houseId, UUID landlordId);

    List<Floor> findByHouseIdAndHouseLandlordIdOrderByFloorNumberAsc(UUID houseId, UUID landlordId);

    boolean existsByHouseIdAndFloorNumber(UUID houseId, int floorNumber);

    boolean existsByHouseIdAndFloorNumberAndIdNot(UUID houseId, int floorNumber, UUID id);

    boolean existsByHouseId(UUID houseId);

    boolean existsByHouseIdAndFloorNumberGreaterThan(UUID houseId, int floorNumber);
}
