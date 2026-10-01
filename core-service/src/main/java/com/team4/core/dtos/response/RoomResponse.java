package com.team4.core.dtos.response;

import com.team4.core.entities.Room;
import com.team4.core.enums.RoomStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RoomResponse(UUID id, UUID floorId, UUID houseId, String roomNumber, BigDecimal area,
        BigDecimal basePrice, int maxTenants, RoomStatus status, boolean hasPrivateBathroom,
        boolean hasAirConditioner, boolean hasWaterHeater, boolean hasBalcony,
        String amenitiesDescription, Instant createdAt, Instant updatedAt) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getFloor().getId(), room.getFloor().getHouse().getId(),
                room.getRoomNumber(), room.getArea(), room.getBasePrice(), room.getMaxTenants(), room.getStatus(),
                room.isHasPrivateBathroom(), room.isHasAirConditioner(), room.isHasWaterHeater(),
                room.isHasBalcony(), room.getAmenitiesDescription(), room.getCreatedAt(), room.getUpdatedAt());
    }
}
