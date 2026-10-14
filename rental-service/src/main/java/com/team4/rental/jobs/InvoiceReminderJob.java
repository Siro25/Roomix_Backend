package com.team4.rental.jobs;

import com.team4.rental.repositories.*;
import com.team4.rental.services.InvoiceReminderService;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "rental.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class InvoiceReminderJob {
    private final InvoiceRepository invoices;
    private final TenancyRepository tenancies;
    private final InvoiceReminderService reminderService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${rental.jobs.reminder-delay-ms:60000}")
    public void remindOverdueInvoices() {
        for (var invoiceId : invoices.findInvoicesNeedingReminder(LocalDate.now(clock), PageRequest.of(0, 100))) {
            try {
                var tenancyId = invoices.findTenancyId(invoiceId).orElseThrow();
                var tenancy = tenancies.findById(tenancyId).orElseThrow();
                reminderService.remind(tenancy.getLandlordId(), invoiceId);
            } catch (RuntimeException exception) {
                log.warn("Không thể tạo nhắc hóa đơn {}", invoiceId, exception);
            }
        }
    }
}
