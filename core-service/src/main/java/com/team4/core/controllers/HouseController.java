package com.team4.core.controllers;

import com.team4.core.dtos.request.HouseRequest;
import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.HouseResponse;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.services.HouseService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
public class HouseController {
    private final HouseService houseService;

    @PostMapping
    public ResponseEntity<ApiResponse<HouseResponse>> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody HouseRequest request) {
        var house = houseService.create(UUID.fromString(jwt.getSubject()), request);
        return ResponseEntity.created(URI.create("/api/houses/" + house.id()))
                .body(success(house, "Tạo nhà thành công"));
    }

    @GetMapping
    public ApiResponse<PageResponse<HouseResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String ward,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return success(houseService.list(UUID.fromString(jwt.getSubject()), keyword, city, ward,
                page, size, sort), "Thành công");
    }

    @GetMapping("/{houseId}")
    public ApiResponse<HouseResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID houseId) {
        return success(houseService.get(UUID.fromString(jwt.getSubject()), houseId), "Thành công");
    }

    @PatchMapping("/{houseId}")
    public ApiResponse<HouseResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID houseId,
            @RequestBody HouseRequest request) {
        return success(houseService.update(UUID.fromString(jwt.getSubject()), houseId, request),
                "Cập nhật nhà thành công");
    }

    @DeleteMapping("/{houseId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID houseId) {
        houseService.delete(UUID.fromString(jwt.getSubject()), houseId);
        return ResponseEntity.noContent().build();
    }

    private <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder().data(data).message(message).build();
    }
}
