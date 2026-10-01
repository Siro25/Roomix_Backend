package com.team4.core.services;

import java.util.UUID;

public interface AccountNotificationService {
    void send(UUID userId, String event, String title, String content);
}
