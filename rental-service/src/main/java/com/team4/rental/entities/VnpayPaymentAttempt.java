package com.team4.rental.entities;

import com.team4.rental.enums.VnpayPaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "vnpay_payment_attempts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayPaymentAttempt {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID invoiceId;

    @Column(nullable = false, unique = true, length = 100)
    private String txnRef;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VnpayPaymentStatus status;

    @Column(nullable = false, columnDefinition = "text")
    private String paymentUrl;

    @Column(length = 10)
    private String responseCode;

    @Column(length = 50)
    private String transactionNo;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant completedAt;
}
