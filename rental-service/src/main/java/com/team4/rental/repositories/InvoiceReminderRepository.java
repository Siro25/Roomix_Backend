package com.team4.rental.repositories;

import com.team4.rental.entities.InvoiceReminder;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InvoiceReminderRepository extends JpaRepository<InvoiceReminder, UUID> {
    Optional<InvoiceReminder> findByInvoiceIdAndReminderDate(UUID invoiceId, LocalDate reminderDate);

    @Query("""
            select r from InvoiceReminder r, Invoice i where r.invoiceId = i.id
              and (r.tenantId = :userId or r.landlordId = :userId)
              and i.paidAmount < i.totalAmount and i.dueDate < :today
              and r.reminderDate = (select max(r2.reminderDate) from InvoiceReminder r2 where r2.invoiceId = i.id)
            """)
    Page<InvoiceReminder> findVisible(UUID userId, LocalDate today, Pageable pageable);
}
