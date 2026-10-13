package com.team4.rental.repositories;

import com.team4.rental.entities.Invoice;
import com.team4.rental.enums.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByTenancyIdAndBillingMonth(UUID tenancyId, String billingMonth);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> lockById(UUID id);

    @Query("""
            select i from Invoice i, Tenancy t
            where i.tenancyId = t.id and (t.landlordId = :userId or t.tenantId = :userId)
            """)
    Page<Invoice> findVisible(UUID userId, Pageable pageable);

    @Query("""
            select i from Invoice i, Tenancy t
            where i.id = :id and i.tenancyId = t.id
                and (t.landlordId = :userId or t.tenantId = :userId)
            """)
    Optional<Invoice> findVisibleById(UUID id, UUID userId);
}
