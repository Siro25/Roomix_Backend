package com.team4.core.repositories;

import com.team4.core.entities.Notification;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findAllByUserId(UUID userId, Pageable pageable);

    long countByUserIdAndReadFalse(UUID userId);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification notification
            set notification.read = true, notification.readAt = :readAt
            where notification.userId = :userId and notification.read = false
            """)
    int markAllAsRead(
            @Param("userId") UUID userId,
            @Param("readAt") Instant readAt);
}
