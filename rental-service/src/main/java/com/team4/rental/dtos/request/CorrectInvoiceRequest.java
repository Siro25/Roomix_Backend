package com.team4.rental.dtos.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorrectInvoiceRequest {
    @NotNull @Min(0)
    private Integer expectedRevision;

    @NotBlank @Size(max = 2000)
    private String reason;

    @NotNull @PastOrPresent
    private LocalDate readingDate;
    @NotNull
    private LocalDate dueDate;

    @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2)
    private BigDecimal roomRent;
    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    private BigDecimal electricityUnitPrice;
    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    private BigDecimal waterUnitPrice;

    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    private BigDecimal newElectricityVal;
    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    private BigDecimal newWaterVal;
}
