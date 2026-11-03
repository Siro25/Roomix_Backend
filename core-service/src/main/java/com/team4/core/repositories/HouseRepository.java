package com.team4.core.repositories;

import com.team4.core.entities.House;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HouseRepository extends JpaRepository<House, UUID> {
    List<House> findDistinctByLandlordIdOrderByCreatedAtDesc(UUID landlordId);

    Optional<House> findByIdAndLandlordId(UUID id, UUID landlordId);

    @EntityGraph(attributePaths = "floors")
    @Query("""
            select distinct house
            from House house
            where house.landlordId = :landlordId
            order by house.createdAt desc
            """)
    List<House> findOverviewByLandlordId(@Param("landlordId") UUID landlordId);

    long countByLandlordId(UUID landlordId);
}
