package com.team4.core.services.impl;

import com.team4.core.dtos.request.AuditContext;
import com.team4.core.dtos.request.CreateReportRequest;
import com.team4.core.dtos.request.ReviewReportRequest;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.dtos.response.ViolationReportResponse;
import com.team4.core.entities.ViolationReport;
import com.team4.core.enums.ReportStatus;
import com.team4.core.enums.NotificationType;
import com.team4.core.enums.Role;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.ViolationReportRepository;
import com.team4.core.repositories.UserRepository;
import com.team4.core.services.AccountNotificationService;
import com.team4.core.services.AuditService;
import com.team4.core.services.ViolationReportService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViolationReportServiceImpl implements ViolationReportService {
    private static final String REPORT_ENTITY = "VIOLATION_REPORT";

    private final ViolationReportRepository violationReportRepository;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final AccountNotificationService notificationService;

    @Override
    @Transactional
    public ViolationReportResponse create(UUID reporterId, CreateReportRequest request) {
        ViolationReport report = violationReportRepository.save(ViolationReport.builder()
                .reporterId(reporterId)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason().strip())
                .description(request.getDescription() == null ? null : request.getDescription().strip())
                .status(ReportStatus.PENDING)
                .build());

        auditService.record(
                "CREATE_REPORT",
                REPORT_ENTITY,
                report.getId(),
                Map.of(),
                Map.of("status", report.getStatus().name(), "targetType", report.getTargetType().name()),
                AuditContext.builder().actorId(reporterId).build());
        userRepository.findAllByRole(Role.ADMIN).forEach(admin -> notificationService.send(
                admin.getId(),
                report.getId(),
                NotificationType.GENERAL,
                "VIOLATION_REPORT_CREATED",
                "Có báo cáo vi phạm mới",
                "Một báo cáo " + report.getTargetType().name()
                        + " mới đang chờ quản trị viên xử lý.",
                "/admin/reports"));
        return ViolationReportResponse.from(report);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ViolationReportResponse> mine(UUID reporterId, Pageable pageable) {
        return PageResponse.from(
                violationReportRepository.findByReporterId(reporterId, pageable),
                ViolationReportResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ViolationReportResponse> search(ReportStatus status, Pageable pageable) {
        Page<ViolationReport> reports = status == null
                ? violationReportRepository.findAll(pageable)
                : violationReportRepository.findByStatus(status, pageable);
        return PageResponse.from(reports, ViolationReportResponse::from);
    }

    @Override
    @Transactional
    public ViolationReportResponse review(
            UUID adminId,
            UUID reportId,
            ReviewReportRequest request,
            AuditContext context) {
        ViolationReport report = violationReportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
        if (request.getStatus() == ReportStatus.PENDING) {
            throw new AppException(ErrorCode.REPORT_STATUS_INVALID);
        }

        ReportStatus oldStatus = report.getStatus();
        report.setStatus(request.getStatus());
        report.setResolution(request.getResolution() == null ? null : request.getResolution().strip());
        report.setReviewedBy(adminId);
        report.setReviewedAt(Instant.now());
        violationReportRepository.save(report);

        auditService.record(
                "REVIEW_REPORT",
                REPORT_ENTITY,
                report.getId(),
                Map.of("status", oldStatus.name()),
                Map.of("status", report.getStatus().name()),
                context);
        notificationService.send(
                report.getReporterId(),
                report.getId(),
                NotificationType.GENERAL,
                "VIOLATION_REPORT_REVIEWED",
                "Báo cáo vi phạm đã được xử lý",
                "Báo cáo của bạn đã chuyển sang trạng thái " + report.getStatus().name() + ".",
                "/notifications");
        return ViolationReportResponse.from(report);
    }
}
