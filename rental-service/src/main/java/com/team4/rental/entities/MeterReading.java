package com.team4.rental.entities;

import com.team4.rental.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "meter_readings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterReading {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "contract_id", nullable = false)
    private UUID tenancyId;

    @Column(nullable = false)
    private UUID roomId;

    @Column(nullable = false, length = 7)
    private String billingMonth;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal oldElectricityVal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal newElectricityVal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal electricityUsage;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal oldWaterVal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal newWaterVal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal waterUsage;

    @Column(nullable = false)
    private UUID recordedBy;

    @Column(nullable = false)
    private LocalDate readingDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
