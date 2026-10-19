package com.team4.core.controllers;

import com.team4.core.dtos.request.LandlordRoomRequest;
import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.LandlordRoomResponse;
import com.team4.core.services.LandlordRoomService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/landlords/rooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LANDLORD')")
public class LandlordRoomController {
    private final LandlordRoomService landlordRoomService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LandlordRoomResponse> create(
            @Valid @RequestPart("data") LandlordRoomRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal Jwt jwt) {
        LandlordRoomResponse response = landlordRoomService.create(
                UUID.fromString(jwt.getSubject()), request, images);
        return ApiResponse.<LandlordRoomResponse>builder()
                .message("Lưu nháp phòng thành công")
                .data(response)
                .build();
    }

    @PutMapping(value = "/{roomId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LandlordRoomResponse> update(
            @PathVariable UUID roomId,
            @Valid @RequestPart("data") LandlordRoomRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal Jwt jwt) {
        LandlordRoomResponse response = landlordRoomService.update(
                UUID.fromString(jwt.getSubject()), roomId, request, images);
        return ApiResponse.<LandlordRoomResponse>builder()
                .message("Cập nhật phòng thành công")
                .data(response)
                .build();
    }

    @PostMapping("/{roomId}/submit")
    public ApiResponse<LandlordRoomResponse> submit(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal Jwt jwt) {
        LandlordRoomResponse response = landlordRoomService.submit(
                UUID.fromString(jwt.getSubject()), roomId);
        return ApiResponse.<LandlordRoomResponse>builder()
                .message("Gửi phòng chờ duyệt thành công")
                .data(response)
                .build();
    }

    @PostMapping("/{roomId}/hide")
    public ApiResponse<LandlordRoomResponse> hide(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal Jwt jwt) {
        LandlordRoomResponse response = landlordRoomService.hide(
                UUID.fromString(jwt.getSubject()), roomId);
        return ApiResponse.<LandlordRoomResponse>builder()
                .message("Ẩn phòng thành công")
                .data(response)
                .build();
    }

    @PostMapping("/{roomId}/unhide")
    public ApiResponse<LandlordRoomResponse> unhide(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal Jwt jwt) {
        LandlordRoomResponse response = landlordRoomService.unhide(
                UUID.fromString(jwt.getSubject()), roomId);
        return ApiResponse.<LandlordRoomResponse>builder()
                .message("Hiện lại phòng thành công")
                .data(response)
                .build();
    }
}
