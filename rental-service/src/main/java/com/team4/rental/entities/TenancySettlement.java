package com.team4.rental.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tenancy_settlements")
@Immutable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenancySettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "contract_id", nullable = false, unique = true)
    private UUID tenancyId;
    @Column(nullable = false)
    private LocalDate actualReturnDate;
    @Column(length = 7)
    private String lastBilledMonth;
    @Column(nullable = false)
    private int unbilledMonths;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal previousElectricity;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal finalElectricity;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal previousWater;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal finalWater;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyRent;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal electricityUnitPrice;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal waterUnitPrice;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal damageCharge;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal invoiceDebt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal additionalRent;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal additionalElectricity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal additionalWater;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDue;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal depositAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal depositApplied;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountToCollect;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountToRefund;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal collectedAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount;

    @Column(length = 2000)
    private String damageReason;
    @Column(nullable = false, length = 30)
    private String paymentMethod;
    @Column(nullable = false)
    private UUID settledBy;
    @Column(nullable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, BigDecimal> invoiceBalances;
}
