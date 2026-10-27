package com.team4.core.controllers;

import com.team4.core.dtos.request.UpdateProfileRequest;
import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.LandlordContactResponse;
import com.team4.core.dtos.response.UserProfileResponse;
import com.team4.core.services.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal Jwt jwt) {
        UserProfileResponse response = userService.getProfile(UUID.fromString(jwt.getSubject()));
        return ApiResponse.<UserProfileResponse>builder()
                .data(response)
                .message("Thành công")
                .build();
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UserProfileResponse response = userService.updateProfile(
                UUID.fromString(jwt.getSubject()),
                request);

        return ApiResponse.<UserProfileResponse>builder()
                .message("Cập nhật hồ sơ thành công")
                .data(response)
                .build();
    }

    @GetMapping("/landlords/{landlordId}")
    public ApiResponse<LandlordContactResponse> landlordContact(@PathVariable UUID landlordId) {
        LandlordContactResponse response = userService.getLandlordContact(landlordId);

        return ApiResponse.<LandlordContactResponse>builder()
                .message("Lấy thông tin liên hệ chủ trọ thành công")
                .data(response)
                .build();
    }
}
