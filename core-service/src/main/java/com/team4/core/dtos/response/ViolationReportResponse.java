package com.team4.core.dtos.response;

import com.team4.core.entities.ViolationReport;
import com.team4.core.enums.ReportStatus;
import com.team4.core.enums.ReportTargetType;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ViolationReportResponse {
    UUID id;
    UUID reporterId;
    ReportTargetType targetType;
    UUID targetId;
    String reason;
    String description;
    ReportStatus status;
    String resolution;
    UUID reviewedBy;
    Instant reviewedAt;
    Instant createdAt;
    Instant updatedAt;

    public static ViolationReportResponse from(ViolationReport report) {
        return ViolationReportResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporterId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .resolution(report.getResolution())
                .reviewedBy(report.getReviewedBy())
                .reviewedAt(report.getReviewedAt())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
