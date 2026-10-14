package com.team4.rental.dtos.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementPreviewRequest {
    @NotNull @PastOrPresent
    private LocalDate actualReturnDate;

    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    private BigDecimal finalElectricity;
    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    private BigDecimal finalWater;

    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    private BigDecimal damageCharge;
    @Size(max = 2000)
    private String damageReason;
}
