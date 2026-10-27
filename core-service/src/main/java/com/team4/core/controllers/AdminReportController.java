package com.team4.core.controllers;

import com.team4.core.dtos.request.AuditContext;
import com.team4.core.dtos.request.ReviewReportRequest;
import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.dtos.response.ViolationReportResponse;
import com.team4.core.enums.ReportStatus;
import com.team4.core.services.ViolationReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {
    private final ViolationReportService violationReportService;

    @GetMapping
    public ApiResponse<PageResponse<ViolationReportResponse>> search(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<ViolationReportResponse> response = violationReportService.search(status, pageable);

        return ApiResponse.<PageResponse<ViolationReportResponse>>builder()
                .message("Lấy danh sách báo cáo thành công")
                .data(response)
                .build();
    }

    @PostMapping("/{reportId}/review")
    public ApiResponse<ViolationReportResponse> review(
            @PathVariable UUID reportId,
            @Valid @RequestBody ReviewReportRequest requestBody,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        AuditContext context = AuditContext.builder()
                .actorId(adminId)
                .ipAddress(request.getRemoteAddr())
                .userAgent(request.getHeader("User-Agent"))
                .build();

        ViolationReportResponse response = violationReportService.review(
                adminId,
                reportId,
                requestBody,
                context);

        return ApiResponse.<ViolationReportResponse>builder()
                .message("Cập nhật báo cáo thành công")
                .data(response)
                .build();
    }
}
