package com.team4.core.dtos.response;

import com.team4.core.entities.AuditLog;
import java.time.Instant;
import java.util.Map;
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
public class AuditLogResponse {
    UUID id;
    UUID userId;
    String action;
    String entityName;
    String entityId;
    String ipAddress;
    String userAgent;
    Map<String, Object> oldValues;
    Map<String, Object> newValues;
    Instant createdAt;

    public static AuditLogResponse from(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUserId())
                .action(auditLog.getAction())
                .entityName(auditLog.getEntityName())
                .entityId(auditLog.getEntityId())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .oldValues(auditLog.getOldValues())
                .newValues(auditLog.getNewValues())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
