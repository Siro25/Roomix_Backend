package com.team4.rental.dtos.response;

import java.time.Instant;
import lombok.*;

@Getter
@Builder
public class VnpayPaymentResponse {
    private final String txnRef;
    private final String paymentUrl;
    private final Instant expiresAt;
}
