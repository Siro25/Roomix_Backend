package com.team4.core.dtos.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
public class RoomDetailResponse {
    UUID postId;
    UUID roomId;
    UUID houseId;
    UUID floorId;
    String houseName;
    String floorName;
    int floorNumber;
    String roomNumber;
    String title;
    String description;
    BigDecimal rentalPrice;
    BigDecimal depositAmount;
    BigDecimal area;
    int maxTenants;
    LocalDate availableFrom;
    String addressStreet;
    String ward;
    String district;
    String city;
    boolean hasPrivateBathroom;
    boolean hasAirConditioner;
    boolean hasWaterHeater;
    boolean hasBalcony;
    String amenitiesDescription;
    boolean rented;
    boolean available;
    List<String> imageUrls;
    Instant createdAt;
}
