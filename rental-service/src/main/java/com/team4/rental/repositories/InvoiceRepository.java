package com.team4.rental.repositories;

import com.team4.rental.entities.Invoice;
import com.team4.rental.enums.*;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.tenancyId = :tenancyId order by i.billingMonth, i.id")
    List<Invoice> lockAllForTenancy(UUID tenancyId);

    @Query("""
            select i from Invoice i, Tenancy t
            where i.tenancyId = t.id and (t.landlordId = :userId or t.tenantId = :userId)
              and (:status is null or i.status = :status)
              and (:roomId is null or t.roomId = :roomId)
              and (:tenancyId is null or t.id = :tenancyId)
              and (:billingMonth is null or i.billingMonth = :billingMonth)
              and (:overdue is null
                or (:overdue = true and i.dueDate < :today and i.paidAmount < i.totalAmount)
                or (:overdue = false and (i.dueDate >= :today or i.paidAmount >= i.totalAmount)))
            """)
    Page<Invoice> searchVisible(UUID userId, InvoiceStatus status, UUID roomId, UUID tenancyId,
                               String billingMonth, Boolean overdue, LocalDate today, Pageable pageable);

    @Query("select i.tenancyId from Invoice i where i.id = :id")
    Optional<UUID> findTenancyId(UUID id);

    @Query("""
            select i.id from Invoice i where i.dueDate < :today and i.paidAmount < i.totalAmount
              and not exists (select r.id from InvoiceReminder r where r.invoiceId = i.id and r.reminderDate = :today)
            order by i.dueDate, i.id
            """)
    List<UUID> findInvoicesNeedingReminder(LocalDate today, Pageable pageable);

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
