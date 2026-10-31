package com.team4.core.controllers;

import com.team4.core.dtos.response.AdminDashboardResponse;
import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.services.AdminDashboardService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final AdminDashboardService dashboardService;

    @GetMapping
    public ApiResponse<AdminDashboardResponse> getDashboard(
            @AuthenticationPrincipal Jwt jwt) {
        AdminDashboardResponse dashboard = dashboardService.getDashboard(
                UUID.fromString(jwt.getSubject()));

        return ApiResponse.<AdminDashboardResponse>builder()
                .message("Lấy tổng quan quản trị thành công")
                .data(dashboard)
                .build();
    }
}
