package com.team4.rental.dtos.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReasonRequest {
    @NotBlank @Size(max = 2000)
    private String reason;
}
