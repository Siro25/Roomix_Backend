package com.team4.core.dtos.response;

import com.team4.core.enums.PostStatus;
import com.team4.core.enums.RoomStatus;
import java.math.BigDecimal;
import java.time.Instant;
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
public class LandlordPropertyResponse {
    UUID houseId;
    String houseName;
    String address;
    String addressStreet;
    String ward;
    String district;
    String city;
    BigDecimal latitude;
    BigDecimal longitude;
    String description;
    int totalFloors;
    int configuredFloors;
    int totalRooms;
    int availableRooms;
    int rentedRooms;
    Instant createdAt;
    Instant updatedAt;
    List<FloorSummary> floors;

    @Getter
    @Builder
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class FloorSummary {
        UUID floorId;
        int floorNumber;
        String floorName;
        String description;
        int totalRooms;
        int availableRooms;
        int rentedRooms;
        List<RoomSummary> rooms;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class RoomSummary {
        UUID roomId;
        UUID postId;
        String roomNumber;
        String title;
        BigDecimal area;
        BigDecimal basePrice;
        int maxTenants;
        RoomStatus roomStatus;
        PostStatus postStatus;
        String rejectReason;
        boolean rented;
    }
}
