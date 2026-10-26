package com.team4.rental.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteSettlementRequest {
    @Valid @NotNull
    private SettlementPreviewRequest details;

    @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2)
    private BigDecimal expectedTotalDue;
    @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2)
    private BigDecimal collectedAmount;
    @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2)
    private BigDecimal refundedAmount;

    @NotBlank @Pattern(regexp = "CASH|BANK_TRANSFER")
    private String paymentMethod;
}
