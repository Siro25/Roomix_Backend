package com.team4.rental.controllers;

import com.team4.rental.dtos.response.ApiResponse;
import com.team4.rental.dtos.response.LandlordDashboardResponse;
import com.team4.rental.services.LandlordDashboardService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rentals/dashboard")
@RequiredArgsConstructor
public class LandlordDashboardController {
    private final LandlordDashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<LandlordDashboardResponse> getDashboard(
            @AuthenticationPrincipal Jwt jwt) {
        LandlordDashboardResponse dashboard = dashboardService.getDashboard(
                UUID.fromString(jwt.getSubject()),
                jwt.getTokenValue());

        return ApiResponse.<LandlordDashboardResponse>builder()
                .message("Lấy tổng quan chủ trọ thành công")
                .data(dashboard)
                .build();
    }
}
