package com.team4.core.services.impl;

import com.team4.core.dtos.request.AuditContext;
import com.team4.core.events.AuditRecordEvent;
import com.team4.core.services.AuditService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void record(
            String action,
            String entityName,
            UUID entityId,
            Map<String, Object> oldValues,
            Map<String, Object> newValues,
            AuditContext context) {
        eventPublisher.publishEvent(AuditRecordEvent.builder()
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .oldValues(immutableCopy(oldValues))
                .newValues(immutableCopy(newValues))
                .context(context)
                .build());
    }

    private Map<String, Object> immutableCopy(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
