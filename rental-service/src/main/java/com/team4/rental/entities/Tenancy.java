package com.team4.rental.entities;

import com.team4.rental.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenancy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String contractCode;

    @Column(nullable = false, unique = true)
    private UUID rentalRequestId;

    @Column(nullable = false)
    private UUID roomId;

    @Column(nullable = false)
    private UUID landlordId;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private LocalDate actualReturnDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyRent;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal depositAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal electricityUnitPrice;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal waterUnitPrice;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal initialElectricity;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal initialWater;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenancyStatus status;

    @Column(length = 5000)
    private String termsAndConditions;

    @Column(length = 2000)
    private String cancellationReason;

    private Instant depositConfirmedAt;

    @Column(nullable = false)
    private Instant depositDeadline;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
