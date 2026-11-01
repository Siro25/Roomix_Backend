package com.team4.rental.services.impl;

import com.team4.rental.dtos.request.CorrectInvoiceRequest;
import com.team4.rental.dtos.response.InvoiceDetailResponse;
import com.team4.rental.dtos.response.InvoiceSnapshot;
import com.team4.rental.entities.*;
import com.team4.rental.enums.*;
import com.team4.rental.exceptions.*;
import com.team4.rental.repositories.*;
import com.team4.rental.services.AuditService;
import com.team4.rental.services.InvoiceCorrectionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class InvoiceCorrectionServiceImpl implements InvoiceCorrectionService {
    private final TenancyRepository tenancies;
    private final InvoiceRepository invoices;
    private final MeterReadingRepository readings;
    private final InvoiceItemRepository items;
    private final InvoiceCorrectionRepository corrections;
    private final VnpayPaymentAttemptRepository vnpayAttempts;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceCorrection> history(UUID userId, UUID invoiceId, Pageable pageable) {
        invoices.findVisibleById(invoiceId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        return corrections.findByInvoiceIdOrderByRevisionDesc(invoiceId, pageable);
    }

    @Override
    @Transactional
    public InvoiceDetailResponse correct(UUID landlordId, UUID invoiceId, CorrectInvoiceRequest request) {
        UUID tenancyId = invoices.findTenancyId(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        // Same lock order as creation/settlement: contract, then invoice.
        var tenancy = tenancies.lockOwned(tenancyId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        var invoice = invoices.lockById(invoiceId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (vnpayAttempts.findByInvoiceIdAndStatus(invoiceId, VnpayPaymentStatus.PENDING)
                .filter(attempt -> attempt.getExpiresAt().isAfter(clock.instant())).isPresent()) {
            throw new AppException(ErrorCode.VNPAY_PAYMENT_PENDING);
        }
        var reading = readings.findFirstByTenancyIdOrderByBillingMonthDesc(tenancyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (tenancy.getStatus() != TenancyStatus.ACTIVE || invoice.getStatus() == InvoiceStatus.PAID
                || invoice.getPaidAmount().signum() != 0 || !reading.getId().equals(invoice.getMeterReadingId())) {
            throw new AppException(ErrorCode.CORRECTION_NOT_ALLOWED);
        }
        if (invoice.getRevision() != request.getExpectedRevision()) {
            throw new AppException(ErrorCode.DATA_CONFLICT);
        }
        if (!YearMonth.from(request.getReadingDate()).toString().equals(invoice.getBillingMonth())
                || request.getReadingDate().isBefore(tenancy.getStartDate())
                || request.getReadingDate().isAfter(tenancy.getEndDate())
                || request.getReadingDate().isAfter(LocalDate.now(clock))
                || request.getDueDate().isBefore(request.getReadingDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getNewElectricityVal().compareTo(reading.getOldElectricityVal()) < 0
                || request.getNewWaterVal().compareTo(reading.getOldWaterVal()) < 0) {
            throw new InvalidMeterReadingException();
        }
        var invoiceItems = items.findByInvoiceIdOrderByItemType(invoiceId);
        BigDecimal oldTotalAmount = invoice.getTotalAmount();
        int oldRevision = invoice.getRevision();
        var oldValues = objectMapper.convertValue(Map.of("invoice", invoice, "meterReading", reading, "items", invoiceItems),
                InvoiceSnapshot.class);
        BigDecimal electricityUsage = request.getNewElectricityVal().subtract(reading.getOldElectricityVal());
        BigDecimal waterUsage = request.getNewWaterVal().subtract(reading.getOldWaterVal());
        BigDecimal electricityAmount = electricityUsage.multiply(request.getElectricityUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal waterAmount = waterUsage.multiply(request.getWaterUnitPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = request.getRoomRent().add(electricityAmount).add(waterAmount).setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(new BigDecimal("99999999999999999.99")) > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        reading.setNewElectricityVal(request.getNewElectricityVal());
        reading.setElectricityUsage(electricityUsage);
        reading.setNewWaterVal(request.getNewWaterVal());
        reading.setWaterUsage(waterUsage);
        reading.setReadingDate(request.getReadingDate());
        reading.setRecordedBy(landlordId);
        for (var item : invoiceItems) {
            switch (item.getItemType()) {
                case ROOM_RENT -> {
                    item.setQuantity(BigDecimal.ONE);
                    item.setUnitPrice(request.getRoomRent());
                    item.setTotalPrice(request.getRoomRent());
                }
                case ELECTRICITY -> {
                    item.setQuantity(electricityUsage);
                    item.setUnitPrice(request.getElectricityUnitPrice());
                    item.setTotalPrice(electricityAmount);
                }
                case WATER -> {
                    item.setQuantity(waterUsage);
                    item.setUnitPrice(request.getWaterUnitPrice());
                    item.setTotalPrice(waterAmount);
                }
            }
        }
        invoice.setTotalAmount(total);
        invoice.setDueDate(request.getDueDate());
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setConfirmedAt(null);
        invoice.setRevision(invoice.getRevision() + 1);
        // Correction does not silently resolve an existing dispute.
        invoices.saveAndFlush(invoice);
        var newValues = objectMapper.convertValue(Map.of("invoice", invoice, "meterReading", reading, "items", invoiceItems),
                InvoiceSnapshot.class);
        corrections.save(InvoiceCorrection.builder().invoiceId(invoiceId).revision(invoice.getRevision())
                .correctedBy(landlordId).reason(request.getReason().strip())
                .oldValues(oldValues).newValues(newValues).createdAt(clock.instant()).build());
        auditService.record(
                landlordId,
                "CORRECT_INVOICE",
                "INVOICE",
                invoice.getId(),
                Map.of(
                        "revision", oldRevision,
                        "totalAmount", oldTotalAmount.toPlainString()),
                Map.of(
                        "revision", invoice.getRevision(),
                        "totalAmount", invoice.getTotalAmount().toPlainString(),
                        "reason", request.getReason().strip()));
        return InvoiceDetailResponse.builder().invoice(invoice).meterReading(reading).items(invoiceItems)
                .outstandingAmount(total).overdue(invoice.getDueDate().isBefore(LocalDate.now(clock))).build();
    }
}
