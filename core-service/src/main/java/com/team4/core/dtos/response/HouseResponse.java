package com.team4.core.dtos.response;

import com.team4.core.entities.House;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HouseResponse(UUID id, UUID landlordId, String name, String addressStreet,
        String ward, String district, String city, BigDecimal latitude, BigDecimal longitude,
        String description, int totalFloors, Instant createdAt, Instant updatedAt) {
    public static HouseResponse from(House house) {
        return new HouseResponse(house.getId(), house.getLandlord().getId(), house.getName(),
                house.getAddressStreet(), house.getWard(), house.getDistrict(), house.getCity(),
                house.getLatitude(), house.getLongitude(), house.getDescription(), house.getTotalFloors(),
                house.getCreatedAt(), house.getUpdatedAt());
    }
}
