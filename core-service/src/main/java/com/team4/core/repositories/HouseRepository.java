package com.team4.core.repositories;

import com.team4.core.entities.House;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HouseRepository extends JpaRepository<House, UUID>, JpaSpecificationExecutor<House> {
    Optional<House> findByIdAndLandlord_Id(UUID id, UUID landlordId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from House h where h.id = :id and h.landlord.id = :landlordId")
    Optional<House> findOwnedForUpdate(@Param("id") UUID id, @Param("landlordId") UUID landlordId);
}
