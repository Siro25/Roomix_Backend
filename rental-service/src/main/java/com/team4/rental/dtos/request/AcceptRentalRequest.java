package com.team4.rental.dtos.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptRentalRequest {
    @NotNull @FutureOrPresent
    private LocalDate startDate;
    @NotNull @Future
    private LocalDate endDate;
    @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2)
    private BigDecimal monthlyRent;
    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    private BigDecimal depositAmount;
    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    private BigDecimal electricityUnitPrice;
    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    private BigDecimal waterUnitPrice;
    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    private BigDecimal initialElectricity;
    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    private BigDecimal initialWater;
    @Size(max = 5000)
    private String termsAndConditions;
}
