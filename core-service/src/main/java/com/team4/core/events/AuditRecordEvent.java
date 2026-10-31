package com.team4.core.events;

import com.team4.core.dtos.request.AuditContext;
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
public class AuditRecordEvent {
    String action;
    String entityName;
    UUID entityId;
    Map<String, Object> oldValues;
    Map<String, Object> newValues;
    AuditContext context;
}
