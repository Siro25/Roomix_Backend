package com.team4.rental.dtos.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndTenancyRequest {
    @NotNull @PastOrPresent
    private LocalDate actualReturnDate;
}
