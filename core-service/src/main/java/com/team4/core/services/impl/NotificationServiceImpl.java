package com.team4.core.services.impl;

import com.team4.core.dtos.response.NotificationMessage;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.entities.Notification;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.NotificationRepository;
import com.team4.core.services.NotificationService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationMessage> getAll(UUID userId, Pageable pageable) {
        return PageResponse.from(
                notificationRepository.findAllByUserId(userId, pageable),
                notification -> NotificationMessage.builder()
                        .id(notification.getId())
                        .userId(notification.getUserId())
                        .title(notification.getTitle())
                        .content(notification.getContent())
                        .type(notification.getType())
                        .referenceId(notification.getReferenceId())
                        .read(notification.isRead())
                        .readAt(notification.getReadAt())
                        .createdAt(notification.getCreatedAt())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationMessage markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.isRead()) {
            notification.markAsRead(Instant.now());
        }

        return NotificationMessage.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .referenceId(notification.getReferenceId())
                .read(notification.isRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsRead(userId, Instant.now());
    }
}
