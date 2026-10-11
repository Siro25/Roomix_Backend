package com.team4.core.dtos.response;

import com.team4.core.enums.NotificationType;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationMessage {
    UUID id;
    String event;
    UUID userId;
    String title;
    String content;
    NotificationType type;
    UUID referenceId;
    boolean read;
    Instant readAt;
    Instant createdAt;
}
