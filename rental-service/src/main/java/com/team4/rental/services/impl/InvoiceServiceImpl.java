package com.team4.rental.services.impl;

import com.team4.rental.dtos.request.*;
import com.team4.rental.dtos.response.InvoiceDetailResponse;
import com.team4.rental.dtos.response.InvoiceSummaryResponse;
import com.team4.rental.entities.*;
import com.team4.rental.enums.*;
import com.team4.rental.exceptions.*;
import com.team4.rental.repositories.*;
import com.team4.rental.services.InvoiceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {
    private final TenancyRepository tenancies;
    private final MeterReadingRepository meterReadings;
    private final InvoiceRepository invoices;
    private final InvoiceItemRepository invoiceItems;
    private final Clock clock;

    @Override
    @Transactional
    public InvoiceDetailResponse create(UUID landlordId, CreateInvoiceRequest request) {
        // All invoice creation and contract termination serialize on the same contract row.
        var tenancy = tenancies.lockOwned(request.getTenancyId(), landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (tenancy.getStatus() != TenancyStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_STATE);
        }
        YearMonth billingMonth;
        try {
            billingMonth = YearMonth.parse(request.getBillingMonth());
        } catch (DateTimeParseException exception) {
            throw new AppException(ErrorCode.INVALID_BILLING_MONTH);
        }
        if (invoices.findByTenancyIdAndBillingMonth(tenancy.getId(), request.getBillingMonth()).isPresent()) {
            throw new AppException(ErrorCode.DUPLICATE_INVOICE);
        }
        var previous = meterReadings.findFirstByTenancyIdOrderByBillingMonthDesc(tenancy.getId());
        YearMonth nextMonth = previous.map(reading -> YearMonth.parse(reading.getBillingMonth()).plusMonths(1))
                .orElse(YearMonth.from(tenancy.getStartDate()));
        if (!billingMonth.equals(nextMonth) || billingMonth.isAfter(YearMonth.now(clock))
                || billingMonth.isAfter(YearMonth.from(tenancy.getEndDate()))) {
            throw new AppException(ErrorCode.INVALID_BILLING_MONTH);
        }
        if (!YearMonth.from(request.getReadingDate()).equals(billingMonth)
                || request.getReadingDate().isBefore(tenancy.getStartDate())
                || request.getReadingDate().isAfter(tenancy.getEndDate())
                || request.getReadingDate().isAfter(LocalDate.now(clock))
                || request.getDueDate().isBefore(request.getReadingDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        BigDecimal expectedElectricity = previous.map(MeterReading::getNewElectricityVal)
                .orElse(tenancy.getInitialElectricity());
        BigDecimal expectedWater = previous.map(MeterReading::getNewWaterVal).orElse(tenancy.getInitialWater());
        if (request.getOldElectricityVal().compareTo(expectedElectricity) != 0
                || request.getOldWaterVal().compareTo(expectedWater) != 0
                || request.getNewElectricityVal().compareTo(request.getOldElectricityVal()) < 0
                || request.getNewWaterVal().compareTo(request.getOldWaterVal()) < 0) {
            throw new InvalidMeterReadingException();
        }
        BigDecimal electricityUsage = request.getNewElectricityVal().subtract(expectedElectricity);
        BigDecimal waterUsage = request.getNewWaterVal().subtract(expectedWater);
        BigDecimal electricityAmount = electricityUsage.multiply(tenancy.getElectricityUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal waterAmount = waterUsage.multiply(tenancy.getWaterUnitPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = tenancy.getMonthlyRent().add(electricityAmount).add(waterAmount)
                .setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(new BigDecimal("99999999999999999.99")) > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        var reading = meterReadings.save(MeterReading.builder().tenancyId(tenancy.getId())
                .roomId(tenancy.getRoomId()).billingMonth(request.getBillingMonth())
                .oldElectricityVal(expectedElectricity).newElectricityVal(request.getNewElectricityVal())
                .electricityUsage(electricityUsage).oldWaterVal(expectedWater)
                .newWaterVal(request.getNewWaterVal()).waterUsage(waterUsage)
                .recordedBy(landlordId).readingDate(request.getReadingDate()).build());
        var invoice = invoices.save(Invoice.builder().invoiceCode("INV-" + UUID.randomUUID())
                .tenancyId(tenancy.getId()).meterReadingId(reading.getId())
                .billingMonth(request.getBillingMonth()).dueDate(request.getDueDate())
                .totalAmount(total).paidAmount(BigDecimal.ZERO).status(InvoiceStatus.PENDING).build());
        var items = invoiceItems.saveAll(List.of(
                InvoiceItem.builder().invoiceId(invoice.getId()).itemName("Tiền phòng")
                        .itemType(InvoiceItemType.ROOM_RENT).quantity(BigDecimal.ONE)
                        .unitPrice(tenancy.getMonthlyRent()).totalPrice(tenancy.getMonthlyRent()).build(),
                InvoiceItem.builder().invoiceId(invoice.getId()).itemName("Tiền điện")
                        .itemType(InvoiceItemType.ELECTRICITY).quantity(electricityUsage)
                        .unitPrice(tenancy.getElectricityUnitPrice()).totalPrice(electricityAmount).build(),
                InvoiceItem.builder().invoiceId(invoice.getId()).itemName("Tiền nước")
                        .itemType(InvoiceItemType.WATER).quantity(waterUsage)
                        .unitPrice(tenancy.getWaterUnitPrice()).totalPrice(waterAmount).build()));
        return InvoiceDetailResponse.builder().invoice(invoice).meterReading(reading).items(items)
                .overdue(invoice.getDueDate().isBefore(LocalDate.now(clock)))
                .outstandingAmount(total).build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceSummaryResponse> list(UUID userId, InvoiceStatus status, UUID roomId, UUID tenancyId,
                                            String billingMonth, Boolean overdue, Pageable pageable) {
        LocalDate today = LocalDate.now(clock);
        return invoices.searchVisible(userId, status, roomId, tenancyId, billingMonth, overdue, today, pageable)
                .map(invoice -> InvoiceSummaryResponse.builder()
                        .id(invoice.getId()).invoiceCode(invoice.getInvoiceCode()).tenancyId(invoice.getTenancyId())
                        .billingMonth(invoice.getBillingMonth()).dueDate(invoice.getDueDate()).status(invoice.getStatus())
                        .totalAmount(invoice.getTotalAmount()).paidAmount(invoice.getPaidAmount())
                        .outstandingAmount(invoice.getTotalAmount().subtract(invoice.getPaidAmount()))
                        .overdue(invoice.getDueDate().isBefore(today)
                                && invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) < 0)
                        .disputed(invoice.isDisputed()).build());
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDetailResponse get(UUID userId, UUID invoiceId) {
        var invoice = invoices.findVisibleById(invoiceId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        var reading = meterReadings.findById(invoice.getMeterReadingId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        return InvoiceDetailResponse.builder().invoice(invoice).meterReading(reading)
                .overdue(invoice.getDueDate().isBefore(LocalDate.now(clock))
                        && invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) < 0)
                .outstandingAmount(invoice.getTotalAmount().subtract(invoice.getPaidAmount()))
                .items(invoiceItems.findByInvoiceIdOrderByItemType(invoiceId)).build();
    }

    @Override
    @Transactional
    public Invoice confirm(UUID tenantId, UUID invoiceId) {
        var invoice = invoices.lockById(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        var tenancy = tenancies.findVisible(invoice.getTenancyId(), tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (!tenantId.equals(tenancy.getTenantId())) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }
        if (invoice.isDisputed()) {
            throw new AppException(ErrorCode.INVOICE_DISPUTED);
        }
        if (invoice.getStatus() == InvoiceStatus.CONFIRMED) {
            return invoice;
        }
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_STATE);
        }
        invoice.setStatus(InvoiceStatus.CONFIRMED);
        invoice.setConfirmedAt(clock.instant());
        return invoices.save(invoice);
    }

    @Override
    @Transactional
    public Invoice pay(UUID landlordId, UUID invoiceId, PaymentRequest request) {
        var invoice = invoices.lockById(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        tenancies.findByIdAndLandlordId(invoice.getTenancyId(), landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (invoice.isDisputed()) {
            throw new AppException(ErrorCode.INVOICE_DISPUTED);
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return invoice;
        }
        if (invoice.getStatus() != InvoiceStatus.CONFIRMED) {
            throw new AppException(ErrorCode.INVALID_STATE);
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAmount(invoice.getTotalAmount());
        invoice.setPaymentMethod(request.getPaymentMethod());
        invoice.setPaymentDate(clock.instant());
        return invoices.save(invoice);
    }

    @Override
    @Transactional
    public Invoice dispute(UUID tenantId, UUID invoiceId, ReasonRequest request) {
        var invoice = invoices.lockById(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        var tenancy = tenancies.findVisible(invoice.getTenancyId(), tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (!tenantId.equals(tenancy.getTenantId())) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new AppException(ErrorCode.INVALID_STATE);
        }
        invoice.setDisputed(true);
        invoice.setDisputeReason(request.getReason().strip());
        invoice.setDisputeResolution(null);
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setConfirmedAt(null);
        return invoices.save(invoice);
    }

    @Override
    @Transactional
    public Invoice resolveDispute(UUID landlordId, UUID invoiceId, ReasonRequest request) {
        var invoice = invoices.lockById(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        tenancies.findByIdAndLandlordId(invoice.getTenancyId(), landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (!invoice.isDisputed() || invoice.getStatus() == InvoiceStatus.PAID) {
            throw new AppException(ErrorCode.INVALID_STATE);
        }
        invoice.setDisputed(false);
        invoice.setDisputeResolution(request.getReason().strip());
        // Tenant must confirm again after resolution. Resolution never marks an invoice paid.
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setConfirmedAt(null);
        return invoices.save(invoice);
    }
}
