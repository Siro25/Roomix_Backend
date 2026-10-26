package com.team4.rental.services.impl;

import com.team4.rental.dtos.request.*;
import com.team4.rental.entities.*;
import com.team4.rental.enums.*;
import com.team4.rental.exceptions.*;
import com.team4.rental.repositories.*;
import com.team4.rental.services.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenancySettlementServiceImpl implements TenancySettlementService {
    private final TenancyRepository tenancies;
    private final InvoiceRepository invoices;
    private final MeterReadingRepository readings;
    private final TenancySettlementRepository settlements;
    private final VnpayPaymentAttemptRepository vnpayAttempts;
    private final TenancyService tenancyService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public TenancySettlement get(UUID userId, UUID tenancyId) {
        tenancies.findVisible(tenancyId, userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        return settlements.findByTenancyId(tenancyId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    @Override
    @Transactional
    public TenancySettlement preview(UUID landlordId, UUID tenancyId, SettlementPreviewRequest request) {
        UUID roomId = tenancies.findVisibleRoomId(tenancyId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        tenancies.ensureRoomLock(roomId);
        tenancies.lockRoom(roomId);
        var tenancy = tenancies.lockOwned(tenancyId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (tenancy.getStatus() != TenancyStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_STATE);
        }
        if (request.getActualReturnDate().isBefore(tenancy.getStartDate())
                || request.getActualReturnDate().isAfter(LocalDate.now(clock))
                || (request.getDamageCharge().signum() > 0
                    && (request.getDamageReason() == null || request.getDamageReason().isBlank()))) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        var bills = invoices.lockAllForTenancy(tenancyId);
        var latest = readings.findFirstByTenancyIdOrderByBillingMonthDesc(tenancyId);
        YearMonth returnMonth = YearMonth.from(request.getActualReturnDate());
        if (latest.isPresent() && (latest.get().getReadingDate().isAfter(request.getActualReturnDate())
                || YearMonth.parse(latest.get().getBillingMonth()).isAfter(returnMonth))) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        BigDecimal previousElectricity = latest.map(MeterReading::getNewElectricityVal)
                .orElse(tenancy.getInitialElectricity());
        BigDecimal previousWater = latest.map(MeterReading::getNewWaterVal).orElse(tenancy.getInitialWater());
        if (request.getFinalElectricity().compareTo(previousElectricity) < 0
                || request.getFinalWater().compareTo(previousWater) < 0) {
            throw new InvalidMeterReadingException();
        }
        // Existing billing policy: whole calendar months, without prorating.
        // Months already invoiced are excluded even if their invoice remains unpaid.
        YearMonth firstUnbilledMonth = latest.map(value -> YearMonth.parse(value.getBillingMonth()).plusMonths(1))
                .orElse(YearMonth.from(tenancy.getStartDate()));
        int unbilledMonths = Math.toIntExact(Math.max(0, ChronoUnit.MONTHS.between(firstUnbilledMonth, returnMonth) + 1));
        BigDecimal additionalRent = tenancy.getMonthlyRent().multiply(BigDecimal.valueOf(unbilledMonths));
        BigDecimal additionalElectricity = request.getFinalElectricity().subtract(previousElectricity)
                .multiply(tenancy.getElectricityUnitPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal additionalWater = request.getFinalWater().subtract(previousWater)
                .multiply(tenancy.getWaterUnitPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal invoiceDebt = BigDecimal.ZERO;
        Map<String, BigDecimal> invoiceBalances = new LinkedHashMap<>();
        for (var bill : bills) {
            BigDecimal balance = bill.getTotalAmount().subtract(bill.getPaidAmount());
            invoiceBalances.put(bill.getId().toString(), balance);
            invoiceDebt = invoiceDebt.add(balance);
        }
        BigDecimal totalDue = invoiceDebt.add(additionalRent).add(additionalElectricity)
                .add(additionalWater).add(request.getDamageCharge()).setScale(2, RoundingMode.HALF_UP);
        if (totalDue.compareTo(new BigDecimal("99999999999999999.99")) > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        BigDecimal depositApplied = tenancy.getDepositAmount().min(totalDue);
        return TenancySettlement.builder().tenancyId(tenancyId).actualReturnDate(request.getActualReturnDate())
                .lastBilledMonth(latest.map(MeterReading::getBillingMonth).orElse(null)).unbilledMonths(unbilledMonths)
                .previousElectricity(previousElectricity).finalElectricity(request.getFinalElectricity())
                .previousWater(previousWater).finalWater(request.getFinalWater())
                .monthlyRent(tenancy.getMonthlyRent()).electricityUnitPrice(tenancy.getElectricityUnitPrice())
                .waterUnitPrice(tenancy.getWaterUnitPrice()).invoiceDebt(invoiceDebt).invoiceBalances(invoiceBalances)
                .additionalRent(additionalRent).additionalElectricity(additionalElectricity).additionalWater(additionalWater)
                .damageCharge(request.getDamageCharge()).damageReason(request.getDamageReason())
                .totalDue(totalDue).depositAmount(tenancy.getDepositAmount()).depositApplied(depositApplied)
                .amountToCollect(totalDue.subtract(depositApplied))
                .amountToRefund(tenancy.getDepositAmount().subtract(depositApplied)).build();
    }

    @Override
    @Transactional
    public TenancySettlement complete(UUID landlordId, UUID tenancyId, CompleteSettlementRequest request) {
        UUID roomId = tenancies.findVisibleRoomId(tenancyId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        tenancies.ensureRoomLock(roomId);
        tenancies.lockRoom(roomId);
        var tenancy = tenancies.lockOwned(tenancyId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        var existing = settlements.findByTenancyId(tenancyId);
        if (existing.isPresent()) {
            return existing.get();
        }
        // Recalculate using the public preview operation, inside this same transaction.
        // Room/contract/invoice locks remain held until settlement and end both commit.
        var settlement = preview(landlordId, tenancyId, request.getDetails());
        if (request.getExpectedTotalDue().compareTo(settlement.getTotalDue()) != 0
                || request.getCollectedAmount().compareTo(settlement.getAmountToCollect()) != 0
                || request.getRefundedAmount().compareTo(settlement.getAmountToRefund()) != 0) {
            throw new AppException(ErrorCode.PAYMENT_MISMATCH);
        }
        var bills = invoices.lockAllForTenancy(tenancyId);
        for (var bill : bills) {
            if (vnpayAttempts.findByInvoiceIdAndStatus(bill.getId(), VnpayPaymentStatus.PENDING)
                    .filter(attempt -> attempt.getExpiresAt().isAfter(clock.instant())).isPresent()) {
                throw new AppException(ErrorCode.VNPAY_PAYMENT_PENDING);
            }
            if (bill.isDisputed()) {
                throw new AppException(ErrorCode.INVOICE_DISPUTED);
            }
            if (bill.getStatus() != InvoiceStatus.PAID && bill.getStatus() != InvoiceStatus.CONFIRMED) {
                throw new AppException(ErrorCode.INVALID_STATE);
            }
        }
        for (var bill : bills) {
            if (bill.getStatus() != InvoiceStatus.PAID) {
                bill.setPaidAmount(bill.getTotalAmount());
                bill.setStatus(InvoiceStatus.PAID);
                bill.setPaymentDate(clock.instant());
                bill.setPaymentMethod("SETTLEMENT");
            }
        }
        settlement.setCollectedAmount(request.getCollectedAmount());
        settlement.setRefundedAmount(request.getRefundedAmount());
        settlement.setPaymentMethod(request.getPaymentMethod());
        settlement.setSettledBy(landlordId);
        settlement.setCreatedAt(clock.instant());
        settlements.saveAndFlush(settlement);
        tenancyService.end(landlordId, tenancy.getId(), EndTenancyRequest.builder()
                .actualReturnDate(settlement.getActualReturnDate()).build());
        return settlement;
    }
}
