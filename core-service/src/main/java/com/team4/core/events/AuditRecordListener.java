package com.team4.core.events;

import com.team4.core.entities.AuditLog;
import com.team4.core.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditRecordListener {
    private final AuditLogRepository auditLogRepository;

    @Async("auditTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void persist(AuditRecordEvent event) {
        try {
            auditLogRepository.saveAndFlush(AuditLog.builder()
                    .userId(event.getContext().getActorId())
                    .action(event.getAction())
                    .entityName(event.getEntityName())
                    .entityId(event.getEntityId().toString())
                    .oldValues(event.getOldValues())
                    .newValues(event.getNewValues())
                    .ipAddress(event.getContext().getIpAddress())
                    .userAgent(event.getContext().getUserAgent())
                    .build());
        } catch (RuntimeException exception) {
            log.error(
                    "Không thể ghi audit action={}, entity={} id={}",
                    event.getAction(),
                    event.getEntityName(),
                    event.getEntityId(),
                    exception);
        }
    }
}
