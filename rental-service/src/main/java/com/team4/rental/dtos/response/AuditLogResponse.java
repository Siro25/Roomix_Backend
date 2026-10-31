package com.team4.rental.dtos.response;

import com.team4.rental.entities.AuditLog;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditLogResponse {
    UUID id;
    UUID userId;
    String action;
    String entityName;
    String entityId;
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
                .oldValues(auditLog.getOldValues())
                .newValues(auditLog.getNewValues())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
