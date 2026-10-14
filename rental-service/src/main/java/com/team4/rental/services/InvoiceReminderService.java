package com.team4.rental.services;

import com.team4.rental.entities.InvoiceReminder;
import com.team4.rental.exceptions.*;
import com.team4.rental.repositories.*;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceReminderService {
    private final InvoiceRepository invoices;
    private final TenancyRepository tenancies;
    private final InvoiceReminderRepository reminders;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<InvoiceReminder> list(UUID userId, Pageable pageable) {
        return reminders.findVisible(userId, LocalDate.now(clock), pageable);
    }

    @Transactional
    public InvoiceReminder remind(UUID landlordId, UUID invoiceId) {
        var invoice = invoices.lockById(invoiceId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        var tenancy = tenancies.findByIdAndLandlordId(invoice.getTenancyId(), landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        LocalDate today = LocalDate.now(clock);
        if (!invoice.getDueDate().isBefore(today) || invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0) {
            throw new AppException(ErrorCode.NOT_OVERDUE);
        }
        var existing = reminders.findByInvoiceIdAndReminderDate(invoiceId, today);
        if (existing.isPresent()) {
            return existing.get();
        }
        return reminders.save(InvoiceReminder.builder().invoiceId(invoiceId)
                .tenantId(tenancy.getTenantId()).landlordId(landlordId).reminderDate(today)
                .outstandingAmount(invoice.getTotalAmount().subtract(invoice.getPaidAmount()))
                .content("Hóa đơn " + invoice.getInvoiceCode() + " đã quá hạn. Vui lòng kiểm tra và thanh toán.")
                .createdAt(clock.instant()).build());
    }
}
