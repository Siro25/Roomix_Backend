package com.team4.core.repositories;

import com.team4.core.entities.House;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseRepository extends JpaRepository<House, UUID> {
    List<House> findDistinctByLandlordIdOrderByCreatedAtDesc(UUID landlordId);

    Optional<House> findByIdAndLandlordId(UUID id, UUID landlordId);
}
