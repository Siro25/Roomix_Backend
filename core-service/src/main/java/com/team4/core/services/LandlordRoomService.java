package com.team4.core.services;

import com.team4.core.dtos.request.LandlordRoomRequest;
import com.team4.core.dtos.response.LandlordRoomResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface LandlordRoomService {
    LandlordRoomResponse create(
            UUID landlordId,
            LandlordRoomRequest request,
            List<MultipartFile> images);

    LandlordRoomResponse update(
            UUID landlordId,
            UUID roomId,
            LandlordRoomRequest request,
            List<MultipartFile> images);

    LandlordRoomResponse submit(UUID landlordId, UUID roomId);

    LandlordRoomResponse hide(UUID landlordId, UUID roomId);

    LandlordRoomResponse unhide(UUID landlordId, UUID roomId);
}
