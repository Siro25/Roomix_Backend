package com.team4.core.dtos.response;

import com.team4.core.entities.House;
import com.team4.core.entities.Room;
import com.team4.core.enums.RoomStatus;
import java.math.BigDecimal;
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
public class LandlordPropertyResponse {
    UUID houseId;
    String houseName;
    String address;
    int totalFloors;
    List<RoomSummary> rooms;

    public static LandlordPropertyResponse from(House house) {
        var rooms = house.getFloors().stream()
                .flatMap(floor -> floor.getRooms().stream())
                .map(RoomSummary::from)
                .toList();

        return LandlordPropertyResponse.builder()
                .houseId(house.getId())
                .houseName(house.getName())
                .address(String.join(", ",
                        house.getAddressStreet(), house.getWard(), house.getDistrict(), house.getCity()))
                .totalFloors(house.getTotalFloors())
                .rooms(rooms)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RoomSummary {
        UUID id;
        String roomNumber;
        BigDecimal area;
        BigDecimal basePrice;
        int maxTenants;
        RoomStatus status;
        private static RoomSummary from(Room room) {
            return RoomSummary.builder()
                    .id(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .area(room.getArea())
                    .basePrice(room.getBasePrice())
                    .maxTenants(room.getMaxTenants())
                    .status(room.getStatus())
                    .build();
        }
    }
}
