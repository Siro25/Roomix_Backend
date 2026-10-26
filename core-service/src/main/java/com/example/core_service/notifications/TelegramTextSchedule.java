package com.example.core_service.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "core.telegram.worker-enabled", havingValue = "true", matchIfMissing = true)
public class TelegramTextSchedule {
    private static final Logger LOG = LoggerFactory.getLogger(TelegramTextSchedule.class);
    private final TelegramTextWorker worker;
    public TelegramTextSchedule(TelegramTextWorker worker) { this.worker = worker; }

    @Scheduled(fixedDelay = 1000, initialDelay = 3000)
    public void deliver() {
        try { worker.deliverNext(); }
        catch (RuntimeException exception) {
            LOG.warn("Telegram text queue unavailable; worker will retry later");
        }
    }
}
