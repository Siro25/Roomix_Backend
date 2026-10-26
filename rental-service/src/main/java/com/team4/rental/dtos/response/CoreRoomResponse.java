package com.team4.rental.dtos.response;

import java.time.LocalDate;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoreRoomResponse {
    private UUID roomId;
    private UUID postId;
    private UUID landlordId;
    private String postStatus;
    private String roomStatus;
    private Boolean rented;
    private LocalDate availableFrom;
}
