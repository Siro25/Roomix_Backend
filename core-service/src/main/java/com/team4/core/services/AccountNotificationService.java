package com.team4.core.services;

import com.team4.core.enums.NotificationType;
import java.util.UUID;

public interface AccountNotificationService {
    default void send(
            UUID userId,
            UUID referenceId,
            NotificationType type,
            String event,
            String title,
            String content) {
        send(userId, referenceId, type, event, title, content, null);
    }

    void send(
            UUID userId,
            UUID referenceId,
            NotificationType type,
            String event,
            String title,
            String content,
            String targetUrl);
}
