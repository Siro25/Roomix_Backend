package com.team4.core.controllers;

import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.UserProfileResponse;
import com.team4.core.services.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
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
}
