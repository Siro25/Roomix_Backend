package com.team4.core.dtos.response;

import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FloorResponse {
    UUID id;
    UUID houseId;
    int floorNumber;
    String name;
    String description;
}
