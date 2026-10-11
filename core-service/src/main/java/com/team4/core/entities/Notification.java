package com.team4.core.entities;

import com.team4.core.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "notifications")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(nullable = false, length = 150)
    String title;

    @Column(nullable = false, columnDefinition = "text")
    String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    NotificationType type;

    @Column(name = "reference_id")
    UUID referenceId;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    boolean read = false;

    @Column(name = "read_at")
    Instant readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    public void markAsRead(Instant readAt) {
        this.read = true;
        this.readAt = readAt;
    }
}
