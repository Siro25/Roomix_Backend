package com.team4.core.services;

import com.team4.core.dtos.request.AuditContext;
import com.team4.core.dtos.request.CreateReportRequest;
import com.team4.core.dtos.request.ReviewReportRequest;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.dtos.response.ViolationReportResponse;
import com.team4.core.enums.ReportStatus;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface ViolationReportService {
    ViolationReportResponse create(UUID reporterId, CreateReportRequest request);
    PageResponse<ViolationReportResponse> mine(UUID reporterId, Pageable pageable);
    PageResponse<ViolationReportResponse> search(ReportStatus status, Pageable pageable);
    ViolationReportResponse review(UUID adminId, UUID reportId, ReviewReportRequest request, AuditContext context);
}
