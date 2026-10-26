package com.team4.rental.dtos.response;

import java.util.UUID;
import lombok.*;

@Getter
@Builder
public class VnpayReturnResponse {
    private final UUID invoiceId;
    private final String txnRef;
    private final String responseCode;
    private final String paymentStatus;
    private final String message;
}
