package com.team4.core.controllers;

import com.team4.core.dtos.request.RoomRequest;
import com.team4.core.dtos.request.RoomSearchRequest;
import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.dtos.response.RoomResponse;
import com.team4.core.services.RoomService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;

    @PostMapping("/api/floors/{floorId}/rooms")
    public ResponseEntity<ApiResponse<RoomResponse>> create(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID floorId, @Valid @RequestBody RoomRequest request) {
        var room = roomService.create(UUID.fromString(jwt.getSubject()), floorId, request);
        return ResponseEntity.created(URI.create("/api/rooms/" + room.id()))
                .body(success(room, "Tạo phòng thành công"));
    }

    @GetMapping("/api/rooms")
    public ApiResponse<PageResponse<RoomResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @ModelAttribute RoomSearchRequest request) {
        return success(roomService.list(UUID.fromString(jwt.getSubject()), request), "Thành công");
    }

    @GetMapping("/api/rooms/{roomId}")
    public ApiResponse<RoomResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID roomId) {
        return success(roomService.get(UUID.fromString(jwt.getSubject()), roomId), "Thành công");
    }

    @PatchMapping("/api/rooms/{roomId}")
    public ApiResponse<RoomResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID roomId,
            @RequestBody RoomRequest request) {
        return success(roomService.update(UUID.fromString(jwt.getSubject()), roomId, request), "Cập nhật phòng thành công");
    }

    @DeleteMapping("/api/rooms/{roomId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID roomId) {
        roomService.delete(UUID.fromString(jwt.getSubject()), roomId);
        return ResponseEntity.noContent().build();
    }

    private <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder().data(data).message(message).build();
    }
}
