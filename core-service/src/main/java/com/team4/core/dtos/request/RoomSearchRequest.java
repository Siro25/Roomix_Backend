package com.team4.core.dtos.request;

import com.team4.core.enums.RoomStatus;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomSearchRequest {
    private UUID houseId;
    private UUID floorId;
    private RoomStatus status;
    private String keyword;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private int page = 0;
    private int size = 20;
    private String sort = "createdAt,desc";
}
