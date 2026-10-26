package com.team4.rental.entities;

import jakarta.persistence.*;
import com.team4.rental.dtos.response.InvoiceSnapshot;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Immutable;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "invoice_corrections")
@Immutable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCorrection {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID invoiceId;
    @Column(nullable = false, updatable = false)
    private int revision;
    @Column(nullable = false, updatable = false)
    private UUID correctedBy;
    @Column(nullable = false, updatable = false, length = 2000)
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private InvoiceSnapshot oldValues;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private InvoiceSnapshot newValues;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
