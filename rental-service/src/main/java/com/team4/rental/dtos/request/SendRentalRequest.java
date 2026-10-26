package com.team4.rental.dtos.request;

import jakarta.validation.constraints.*;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendRentalRequest {
    @NotNull
    private UUID roomId;
    @Size(max = 2000)
    private String note;
}
