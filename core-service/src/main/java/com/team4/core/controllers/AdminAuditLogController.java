package com.team4.core.controllers;

import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.AuditLogResponse;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {
    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ApiResponse<PageResponse<AuditLogResponse>> list(
            @RequestParam(required = false) String action,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        var auditLogs = action == null || action.isBlank()
                ? auditLogRepository.findAll(pageable)
                : auditLogRepository.findByActionContainingIgnoreCase(action.strip(), pageable);

        return ApiResponse.<PageResponse<AuditLogResponse>>builder()
                .message("Lấy nhật ký kiểm toán thành công")
                .data(PageResponse.from(auditLogs, AuditLogResponse::from))
                .build();
    }
}
