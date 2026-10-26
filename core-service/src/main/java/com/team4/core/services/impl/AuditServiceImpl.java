package com.team4.core.services.impl;

import com.team4.core.dtos.request.AuditContext;
import com.team4.core.entities.AuditLog;
import com.team4.core.repositories.AuditLogRepository;
import com.team4.core.services.AuditService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    private final AuditLogRepository auditLogRepository;

    @Override
    public void record(
            String action,
            String entityName,
            UUID entityId,
            Map<String, Object> oldValues,
            Map<String, Object> newValues,
            AuditContext context) {
        var auditLog = AuditLog.builder()
                .userId(context.getActorId())
                .action(action)
                .entityName(entityName)
                .entityId(entityId.toString())
                .oldValues(oldValues)
                .newValues(newValues)
                .ipAddress(context.getIpAddress())
                .userAgent(context.getUserAgent())
                .build();

        auditLogRepository.save(auditLog);
    }
}
