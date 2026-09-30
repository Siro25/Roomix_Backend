package com.team4.core.repositories;

import com.team4.core.entities.Floor;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FloorRepository extends JpaRepository<Floor, UUID> {
    Page<Floor> findByHouse_Id(UUID houseId, Pageable pageable);
    Optional<Floor> findByIdAndHouse_Landlord_Id(UUID id, UUID landlordId);
    boolean existsByHouse_IdAndFloorNumber(UUID houseId, int floorNumber);
    boolean existsByHouse_IdAndFloorNumberAndIdNot(UUID houseId, int floorNumber, UUID id);
    long countByHouse_Id(UUID houseId);

    // Scalar lookup avoids caching a stale Floor before acquiring the parent lock.
    @Query("select f.house.id from Floor f where f.id = :id and f.house.landlord.id = :landlordId")
    Optional<UUID> findOwnedHouseId(@Param("id") UUID id, @Param("landlordId") UUID landlordId);
}
