package com.team4.core.dtos.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomSummaryResponse {
    UUID postId;
    UUID roomId;
    String title;
    BigDecimal rentalPrice;
    BigDecimal area;
    int maxTenants;
    String addressStreet;
    String ward;
    String district;
    String city;
    boolean hasPrivateBathroom;
    boolean hasAirConditioner;
    boolean hasWaterHeater;
    boolean hasBalcony;
    String amenitiesDescription;
    String primaryImageUrl;
    boolean rented;
    boolean available;
    Instant createdAt;
}
