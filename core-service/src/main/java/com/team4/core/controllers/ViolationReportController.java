package com.team4.core.controllers;

import com.team4.core.dtos.request.CreateReportRequest;
import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.dtos.response.ViolationReportResponse;
import com.team4.core.services.ViolationReportService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ViolationReportController {
    private final ViolationReportService violationReportService;

    @PostMapping
    public ApiResponse<ViolationReportResponse> create(
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID reporterId = UUID.fromString(jwt.getSubject());
        ViolationReportResponse response = violationReportService.create(reporterId, request);

        return ApiResponse.<ViolationReportResponse>builder()
                .message("Gửi báo cáo thành công")
                .data(response)
                .build();
    }

    @GetMapping("/mine")
    public ApiResponse<PageResponse<ViolationReportResponse>> mine(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID reporterId = UUID.fromString(jwt.getSubject());
        PageResponse<ViolationReportResponse> response = violationReportService.mine(reporterId, pageable);

        return ApiResponse.<PageResponse<ViolationReportResponse>>builder()
                .message("Lấy báo cáo của bạn thành công")
                .data(response)
                .build();
    }
}
