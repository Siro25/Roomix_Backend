package com.team4.rental.repositories;

import com.team4.rental.entities.InvoiceCorrection;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceCorrectionRepository extends JpaRepository<InvoiceCorrection, UUID> {
    Page<InvoiceCorrection> findByInvoiceIdOrderByRevisionDesc(UUID invoiceId, Pageable pageable);
}
