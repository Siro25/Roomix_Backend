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
@Table(name = "invoices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String invoiceCode;

    @Column(name = "contract_id", nullable = false)
    private UUID tenancyId;

    @Column(nullable = false, unique = true)
    private UUID meterReadingId;

    @Column(nullable = false, length = 7)
    private String billingMonth;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "is_disputed", nullable = false)
    private boolean disputed;

    @Column(length = 2000)
    private String disputeReason;

    @Column(length = 2000)
    private String disputeResolution;

    private Instant confirmedAt;

    private Instant paymentDate;

    @Column(length = 30)
    private String paymentMethod;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
