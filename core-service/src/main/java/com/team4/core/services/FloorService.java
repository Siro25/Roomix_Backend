package com.team4.core.services;

import com.team4.core.dtos.request.FloorRequest;
import com.team4.core.dtos.response.FloorResponse;
import com.team4.core.dtos.response.PageResponse;
import java.util.UUID;

public interface FloorService {
    FloorResponse create(UUID landlordId, UUID houseId, FloorRequest request);
    PageResponse<FloorResponse> list(UUID landlordId, UUID houseId, int page, int size, String sort);
    FloorResponse get(UUID landlordId, UUID floorId);
    FloorResponse update(UUID landlordId, UUID floorId, FloorRequest request);
    void delete(UUID landlordId, UUID floorId);
}
