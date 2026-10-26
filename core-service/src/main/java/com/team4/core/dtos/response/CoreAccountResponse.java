package com.team4.core.dtos.response;

import java.time.LocalDate;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoreAccountResponse {
    private UUID id;
    private String role;
    private String status;
}
