package com.team4.rental.events;

import com.team4.rental.entities.AuditLog;
import com.team4.rental.repositories.AuditLogRepository;
import java.util.UUID;
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
                    .id(UUID.randomUUID())
                    .userId(event.getUserId())
                    .action(event.getAction())
                    .entityName(event.getEntityName())
                    .entityId(event.getEntityId().toString())
                    .oldValues(event.getOldValues())
                    .newValues(event.getNewValues())
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
