package com.team4.core.services;

import com.team4.core.dtos.request.RoomRequest;
import com.team4.core.dtos.request.RoomSearchRequest;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.dtos.response.RoomResponse;
import java.util.UUID;

public interface RoomService {
    RoomResponse create(UUID landlordId, UUID floorId, RoomRequest request);
    PageResponse<RoomResponse> list(UUID landlordId, RoomSearchRequest request);
    RoomResponse get(UUID landlordId, UUID roomId);
    RoomResponse update(UUID landlordId, UUID roomId, RoomRequest request);
    void delete(UUID landlordId, UUID roomId);
}
