package com.example.core_service.auth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "core.auth.bootstrap.enabled", havingValue = "true")
public class BootstrapAdmin implements ApplicationRunner {
    private final AdminAccountRepository accounts;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;
    private final String email;
    private final String fullName;

    public BootstrapAdmin(AdminAccountRepository accounts, PasswordEncoder encoder,
            @Value("${CORE_ADMIN_USERNAME:}") String username,
            @Value("${CORE_ADMIN_PASSWORD:}") String password,
            @Value("${CORE_ADMIN_EMAIL:}") String email,
            @Value("${CORE_ADMIN_FULL_NAME:}") String fullName) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.username = username.trim();
        this.password = password;
        this.email = email.trim();
        this.fullName = fullName.trim();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (!username.matches("[A-Za-z0-9._-]{4,50}") || password.isBlank()
                || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72
                || email.isBlank() || email.length() > 254 || !email.contains("@")
                || fullName.isBlank() || fullName.length() > 100) {
            throw new IllegalArgumentException("Invalid CORE_ADMIN_* configuration; no account was created");
        }
        // Không đổi mật khẩu/quyền của tài khoản đã có, kể cả username trùng.
        if (accounts.findByUsername(username).isPresent()) {
            throw new IllegalStateException("Account already exists; disable CORE_BOOTSTRAP_ADMIN");
        }
        accounts.insertAdmin(UUID.randomUUID(), username, encoder.encode(password), email, fullName);
    }
}
