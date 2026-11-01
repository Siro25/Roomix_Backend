package com.team4.rental.controllers;

import com.team4.rental.dtos.response.ApiResponse;
import com.team4.rental.dtos.response.AuditLogResponse;
import com.team4.rental.repositories.AuditLogRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rentals/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {
    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ApiResponse<Page<AuditLogResponse>> list(
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending().and(Sort.by("id")));
        var auditLogs = action == null || action.isBlank()
                ? auditLogRepository.findAll(pageable)
                : auditLogRepository.findByActionContainingIgnoreCase(action.strip(), pageable);

        return ApiResponse.<Page<AuditLogResponse>>builder()
                .data(auditLogs.map(AuditLogResponse::from))
                .build();
    }
}
