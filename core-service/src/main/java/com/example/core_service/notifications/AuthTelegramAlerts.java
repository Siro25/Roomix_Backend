package com.example.core_service.notifications;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.example.core_service.auth.AdminAccount;

@Service
public class AuthTelegramAlerts {
    private final TelegramSettings settings;
    private final AuthTelegramRepository queue;
    public AuthTelegramAlerts(TelegramSettings settings, AuthTelegramRepository queue) {
        this.settings = settings; this.queue = queue;
    }

    public void passwordFailed(String username) {
        enqueue(AuthAlertType.PASSWORD_LOGIN_FAILED, "password:" + UUID.randomUUID(), null, username);
    }
    public void passwordSucceeded(AdminAccount account) {
        enqueue(AuthAlertType.PASSWORD_LOGIN_SUCCEEDED, "password:" + UUID.randomUUID(), account.id(), account.username());
    }
    public void faceSucceeded(String sessionId, AdminAccount account) {
        try {
            String key = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(sessionId.getBytes(StandardCharsets.UTF_8)));
            enqueue(AuthAlertType.FACE_LOGIN_SUCCEEDED, "face:" + key, account.id(), account.username());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private void enqueue(AuthAlertType event, String key, UUID adminId, String username) {
        if (!settings.enabled()) return;
        // Username nhập ở lần thất bại chưa được xác thực. Không cho xuống dòng giả nội dung thông báo.
        String clean = username.replaceAll("[\\p{Cntrl}\\p{Cf}]", " ").trim();
        if (clean.length() > 50) clean = clean.substring(0, 50);
        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        String text = "Roomix | " + event.title() + "\n"
                + (adminId == null ? "Tài khoản nhập: " : "Tài khoản: ") + clean + "\n"
                + (adminId == null ? "" : "Admin: " + adminId + "\n")
                + "Thời điểm UTC: " + now + "\nSự kiện: " + id;
        queue.enqueue(id, key, event, text, now);
    }
}
