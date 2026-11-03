package com.team4.core.services;

import com.team4.core.dtos.request.RoomSearchRequest;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.dtos.response.RoomDetailResponse;
import com.team4.core.dtos.response.RoomSummaryResponse;
import com.team4.core.dtos.response.PublicHouseDetailResponse;
import java.util.UUID;

public interface RoomQueryService {
    PageResponse<RoomSummaryResponse> search(RoomSearchRequest request);

    RoomDetailResponse getDetail(UUID roomId);

    PublicHouseDetailResponse getHouseDetail(UUID houseId);
}
