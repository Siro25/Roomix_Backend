package com.team4.core.controllers;

import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.FavoriteResponse;
import com.team4.core.services.FavoriteService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TENANT')")
public class FavoriteController {
    private final FavoriteService favoriteService;

    @PostMapping("/{roomId}")
    public ApiResponse<FavoriteResponse> add(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal Jwt jwt) {
        FavoriteResponse response = favoriteService.add(
                UUID.fromString(jwt.getSubject()), roomId);
        return ApiResponse.<FavoriteResponse>builder()
                .message("Đã thêm phòng vào danh sách yêu thích")
                .data(response)
                .build();
    }

    @DeleteMapping("/{roomId}")
    public ApiResponse<Void> remove(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal Jwt jwt) {
        favoriteService.remove(UUID.fromString(jwt.getSubject()), roomId);
        return ApiResponse.<Void>builder()
                .message("Đã bỏ phòng khỏi danh sách yêu thích")
                .build();
    }

    @GetMapping
    public ApiResponse<List<FavoriteResponse>> getAll(@AuthenticationPrincipal Jwt jwt) {
        List<FavoriteResponse> response = favoriteService.getAll(
                UUID.fromString(jwt.getSubject()));
        return ApiResponse.<List<FavoriteResponse>>builder()
                .message("Lấy danh sách yêu thích thành công")
                .data(response)
                .build();
    }
}
