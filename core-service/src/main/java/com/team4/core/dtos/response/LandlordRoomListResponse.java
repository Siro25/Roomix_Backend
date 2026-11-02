package com.team4.core.dtos.response;

import com.team4.core.enums.PostStatus;
import com.team4.core.enums.RoomStatus;
import java.math.BigDecimal;
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
public class LandlordRoomListResponse {
    UUID landlordId;
    BigDecimal basePrice;
    PostStatus postStatus;
    RoomStatus roomStatus;
    String rejectReason;
    RoomDetailResponse detail;
}
