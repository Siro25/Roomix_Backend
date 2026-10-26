package com.example.core_service.auth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.core_service.notifications.AuthTelegramAlerts;
import com.example.core_service.auth.AuthDtos.AuthResponse;
import com.example.core_service.auth.AuthDtos.UserProfile;

@Service
public class AdminAuthService {
    private final AdminAccountRepository accounts;
    private final PasswordEncoder passwords;
    private final JwtService tokens;
    private final String dummyHash;
    private final AuthTelegramAlerts alerts;

    public AdminAuthService(AdminAccountRepository accounts, PasswordEncoder passwords, JwtService tokens, AuthTelegramAlerts alerts) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.tokens = tokens;
        this.alerts = alerts;
        // Vẫn kiểm tra BCrypt khi username không tồn tại, tránh trả lỗi quá nhanh.
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = AuthFailure.class)
    public AuthResponse login(String username, String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            alerts.passwordFailed(username);
            throw new AuthFailure(401);
        }
        AdminAccount account = accounts.findByUsername(username.trim()).orElse(null);
        boolean matches = passwords.matches(password, account == null ? dummyHash : account.passwordHash());
        if (!matches || account == null || !account.isActiveAdmin()) {
            alerts.passwordFailed(username);
            throw new AuthFailure(401);
        }
        alerts.passwordSucceeded(account);
        return tokens.issuePasswordToken(account);
    }

    public UserProfile profile(String subject) {
        if (subject == null) throw new AuthFailure(401);
        UUID id;
        try { id = UUID.fromString(subject); }
        catch (IllegalArgumentException exception) { throw new AuthFailure(401); }
        if (!id.toString().equals(subject)) throw new AuthFailure(401);
        AdminAccount account = accounts.findById(id).orElseThrow(() -> new AuthFailure(401));
        // Kiểm tra lại DB: JWT còn hạn không cho phép tài khoản đã khóa/mất quyền vào quản trị.
        if (!account.isActiveAdmin()) throw new AuthFailure(403);
        return UserProfile.from(account);
    }
}
