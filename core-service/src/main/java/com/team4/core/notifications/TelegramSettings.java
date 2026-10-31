package com.team4.core.notifications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TelegramSettings {
    private final String token;
    private final String chatId;

    public TelegramSettings(@Value("${core.telegram.bot-token}") String token,
            @Value("${core.telegram.chat-id}") String chatId) {
        this.token = token.trim();
        this.chatId = chatId.trim();
        if (this.token.isEmpty() != this.chatId.isEmpty()) {
            throw new IllegalArgumentException("Telegram requires both TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID");
        }
    }
    public boolean enabled() { return !token.isEmpty(); }
    public String token() { return token; }
    public String chatId() { return chatId; }
}
