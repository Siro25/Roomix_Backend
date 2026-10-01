package com.team4.core.controllers;

import com.team4.core.dtos.request.FloorRequest;
import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.FloorResponse;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.services.FloorService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FloorController {
    private final FloorService floorService;

    @PostMapping("/api/houses/{houseId}/floors")
    public ResponseEntity<ApiResponse<FloorResponse>> create(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID houseId, @Valid @RequestBody FloorRequest request) {
        var floor = floorService.create(UUID.fromString(jwt.getSubject()), houseId, request);
        return ResponseEntity.created(URI.create("/api/floors/" + floor.id()))
                .body(success(floor, "Tạo tầng thành công"));
    }

    @GetMapping("/api/houses/{houseId}/floors")
    public ApiResponse<PageResponse<FloorResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID houseId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "floorNumber,asc") String sort) {
        return success(floorService.list(UUID.fromString(jwt.getSubject()), houseId, page, size, sort),
                "Thành công");
    }

    @GetMapping("/api/floors/{floorId}")
    public ApiResponse<FloorResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID floorId) {
        return success(floorService.get(UUID.fromString(jwt.getSubject()), floorId), "Thành công");
    }

    @PatchMapping("/api/floors/{floorId}")
    public ApiResponse<FloorResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID floorId,
            @RequestBody FloorRequest request) {
        return success(floorService.update(UUID.fromString(jwt.getSubject()), floorId, request),
                "Cập nhật tầng thành công");
    }

    @DeleteMapping("/api/floors/{floorId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID floorId) {
        floorService.delete(UUID.fromString(jwt.getSubject()), floorId);
        return ResponseEntity.noContent().build();
    }

    private <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder().data(data).message(message).build();
    }
}
