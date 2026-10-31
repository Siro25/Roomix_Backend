package com.team4.rental.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoreLandlordDashboardResponse {
    long totalHouses;
    long totalRooms;
    long availableRooms;
    long occupiedRooms;
    long reservedRooms;
    long maintenanceRooms;
}
