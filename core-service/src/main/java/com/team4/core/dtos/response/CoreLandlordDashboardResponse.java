package com.team4.core.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CoreLandlordDashboardResponse {
    long totalHouses;
    long totalRooms;
    long availableRooms;
    long occupiedRooms;
    long reservedRooms;
    long maintenanceRooms;
}
