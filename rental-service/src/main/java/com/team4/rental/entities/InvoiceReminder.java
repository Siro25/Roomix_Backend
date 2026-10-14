package com.team4.rental.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "invoice_reminders", uniqueConstraints = @UniqueConstraint(columnNames = {"invoice_id", "reminder_date"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceReminder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private UUID invoiceId;
    @Column(nullable = false)
    private UUID tenantId;
    @Column(nullable = false)
    private UUID landlordId;
    @Column(nullable = false)
    private LocalDate reminderDate;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount;
    @Column(nullable = false, length = 500)
    private String content;
    @Column(nullable = false)
    private Instant createdAt;
}
