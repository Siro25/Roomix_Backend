package com.team4.rental.dtos.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    @NotBlank @Pattern(regexp = "CASH|BANK_TRANSFER")
    private String paymentMethod;
}
