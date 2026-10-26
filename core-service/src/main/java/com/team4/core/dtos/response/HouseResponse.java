package com.team4.core.dtos.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HouseResponse {
    UUID id;
    String name;
    String addressStreet;
    String ward;
    String district;
    String city;
    BigDecimal latitude;
    BigDecimal longitude;
    String description;
    int totalFloors;
    Instant createdAt;
    Instant updatedAt;
    List<FloorResponse> floors;
}
