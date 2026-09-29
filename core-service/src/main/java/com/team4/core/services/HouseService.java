package com.team4.core.services;

import com.team4.core.dtos.request.HouseRequest;
import com.team4.core.dtos.response.HouseResponse;
import com.team4.core.dtos.response.PageResponse;
import java.util.UUID;

public interface HouseService {
    HouseResponse create(UUID landlordId, HouseRequest request);
    PageResponse<HouseResponse> list(UUID landlordId, String keyword, String city, String ward,
            int page, int size, String sort);
    HouseResponse get(UUID landlordId, UUID houseId);
    HouseResponse update(UUID landlordId, UUID houseId, HouseRequest request);
    void delete(UUID landlordId, UUID houseId);
}
