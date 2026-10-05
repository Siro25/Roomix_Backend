package com.team4.core.services;

import com.team4.core.dtos.response.NotificationMessage;
import com.team4.core.dtos.response.PageResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    PageResponse<NotificationMessage> getAll(UUID userId, Pageable pageable);

    long getUnreadCount(UUID userId);

    NotificationMessage markAsRead(UUID userId, UUID notificationId);

    int markAllAsRead(UUID userId);
}
