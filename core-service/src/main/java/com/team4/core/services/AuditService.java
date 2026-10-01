package com.team4.core.services;

import com.team4.core.dtos.request.AuditContext;
import java.util.Map;
import java.util.UUID;

public interface AuditService {
    void record(
            String action,
            String entityName,
            UUID entityId,
            Map<String, Object> oldValues,
            Map<String, Object> newValues,
            AuditContext context);
}
