package com.team4.core.dtos.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PublicHouseDetailResponse {
    UUID houseId;
    UUID landlordId;
    String landlordName;
    String contactPhone;
    String houseName;
    String addressStreet;
    String ward;
    String district;
    String city;
    String description;
    BigDecimal minimumPrice;
    int availableRooms;
    int totalRooms;
    String primaryImageUrl;
    List<FloorGroup> floors;

    @Getter
    @Builder
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class FloorGroup {
        UUID floorId;
        int floorNumber;
        List<RoomDetailResponse> rooms;
    }
}
