package com.team4.rental.dtos.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceRequest {
    @NotNull
    private UUID tenancyId;
    @NotBlank @Pattern(regexp = "[0-9]{4}-(0[1-9]|1[0-2])")
    private String billingMonth;
    @NotNull
    private LocalDate dueDate;
    @NotNull @PastOrPresent
    private LocalDate readingDate;
    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    private BigDecimal oldElectricityVal;
    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    private BigDecimal newElectricityVal;
    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    private BigDecimal oldWaterVal;
    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    private BigDecimal newWaterVal;
}
