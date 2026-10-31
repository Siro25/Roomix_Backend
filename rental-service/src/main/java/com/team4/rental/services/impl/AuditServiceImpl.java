package com.team4.rental.services.impl;

import com.team4.rental.events.AuditRecordEvent;
import com.team4.rental.services.AuditService;
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
            UUID userId,
            String action,
            String entityName,
            UUID entityId,
            Map<String, Object> oldValues,
            Map<String, Object> newValues) {
        eventPublisher.publishEvent(AuditRecordEvent.builder()
                .userId(userId)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .oldValues(immutableCopy(oldValues))
                .newValues(immutableCopy(newValues))
                .build());
    }

    private Map<String, Object> immutableCopy(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
