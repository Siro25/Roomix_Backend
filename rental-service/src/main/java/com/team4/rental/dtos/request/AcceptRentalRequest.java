package com.team4.rental.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AcceptRentalRequest {
    @NotNull
    @FutureOrPresent
    LocalDate startDate;

    @NotNull
    @Future
    LocalDate endDate;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 13, fraction = 2)
    BigDecimal monthlyRent;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 13, fraction = 2)
    BigDecimal depositAmount;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 13, fraction = 2)
    BigDecimal electricityUnitPrice;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 13, fraction = 2)
    BigDecimal waterUnitPrice;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 13, fraction = 2)
    BigDecimal initialElectricity;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 13, fraction = 2)
    BigDecimal initialWater;

    @Size(max = 5000)
    String termsAndConditions;

    @Size(max = 1000)
    String contactNote;

    @Future
    Instant depositDeadline;
}
