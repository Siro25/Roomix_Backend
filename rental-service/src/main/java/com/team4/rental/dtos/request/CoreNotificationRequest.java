package com.team4.rental.dtos.request;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoreNotificationRequest {
    UUID userId;
    UUID referenceId;
    String type;
    String event;
    String title;
    String content;
    String targetUrl;
}
