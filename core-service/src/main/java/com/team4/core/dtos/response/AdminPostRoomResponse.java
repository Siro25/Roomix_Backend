package com.team4.core.dtos.response;

import com.team4.core.entities.Post;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class AdminPostRoomResponse {
    UUID postId;
    UUID roomId;
    UUID floorId;
    int floorNumber;
    String roomNumber;
    String title;
    String description;
    BigDecimal rentalPrice;
    BigDecimal depositAmount;
    BigDecimal area;
    int maxTenants;
    LocalDate availableFrom;
    boolean rented;
    boolean hasPrivateBathroom;
    boolean hasAirConditioner;
    boolean hasWaterHeater;
    boolean hasBalcony;
    String amenitiesDescription;
    List<String> imageUrls;

    public static AdminPostRoomResponse from(Post post, List<String> imageUrls) {
        var room = post.getRoom();
        return AdminPostRoomResponse.builder()
                .postId(post.getId())
                .roomId(room.getId())
                .floorId(room.getFloor().getId())
                .floorNumber(room.getFloor().getFloorNumber())
                .roomNumber(room.getRoomNumber())
                .title(post.getTitle())
                .description(post.getDescription())
                .rentalPrice(post.getRentalPrice())
                .depositAmount(post.getDepositAmount())
                .area(room.getArea())
                .maxTenants(room.getMaxTenants())
                .availableFrom(post.getAvailableFrom())
                .rented(room.isRented())
                .hasPrivateBathroom(room.isHasPrivateBathroom())
                .hasAirConditioner(room.isHasAirConditioner())
                .hasWaterHeater(room.isHasWaterHeater())
                .hasBalcony(room.isHasBalcony())
                .amenitiesDescription(room.getAmenitiesDescription())
                .imageUrls(imageUrls)
                .build();
    }
}
