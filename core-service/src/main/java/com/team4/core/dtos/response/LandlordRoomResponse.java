package com.team4.core.dtos.response;

import com.team4.core.enums.PostStatus;
import com.team4.core.enums.RoomStatus;
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
public class LandlordRoomResponse {
    UUID roomId;
    UUID postId;
    UUID houseId;
    UUID floorId;
    String roomNumber;
    BigDecimal area;
    BigDecimal basePrice;
    RoomStatus roomStatus;
    PostStatus postStatus;
    boolean rented;
    List<String> imageUrls;
}
