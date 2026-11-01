package com.team4.rental.services;

import java.util.Map;
import java.util.UUID;

public interface AuditService {
    void record(
            UUID userId,
            String action,
            String entityName,
            UUID entityId,
            Map<String, Object> oldValues,
            Map<String, Object> newValues);
}
