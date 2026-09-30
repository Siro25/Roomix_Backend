package com.team4.core.dtos.response;

import com.team4.core.entities.Floor;
import java.util.UUID;

public record FloorResponse(UUID id, UUID houseId, int floorNumber, String name, String description) {
    public static FloorResponse from(Floor floor) {
        return new FloorResponse(floor.getId(), floor.getHouse().getId(), floor.getFloorNumber(),
                floor.getName(), floor.getDescription());
    }
}
