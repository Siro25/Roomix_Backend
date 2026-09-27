package com.team4.core.services.impl;

import com.team4.core.dtos.response.NotificationMessage;
import com.team4.core.entities.Notification;
import com.team4.core.enums.NotificationType;
import com.team4.core.repositories.NotificationRepository;
import com.team4.core.services.AccountNotificationService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class AccountNotificationServiceImpl implements AccountNotificationService {
    private static final String USER_QUEUE = "/queue/notifications";

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void send(UUID userId, String event, String title, String content) {
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .type(NotificationType.SYSTEM)
                .referenceId(userId)
                .build());

        var message = NotificationMessage.builder()
                .event(event)
                .userId(userId)
                .title(title)
                .content(content)
                .createdAt(Instant.now())
                .build();

        publishAfterCommit(userId, message);
    }

    private void publishAfterCommit(UUID userId, NotificationMessage message) {
        Runnable publisher = () -> messagingTemplate.convertAndSendToUser(
                userId.toString(), USER_QUEUE, message);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publisher.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.run();
            }
        });
    }
}
