package com.team4.rental.repositories;

import com.team4.rental.entities.TenancySettlement;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenancySettlementRepository extends JpaRepository<TenancySettlement, UUID> {
    Optional<TenancySettlement> findByTenancyId(UUID tenancyId);
}
