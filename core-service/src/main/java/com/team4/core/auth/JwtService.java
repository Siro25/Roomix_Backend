package com.team4.core.auth;

import java.time.Duration;
import java.time.Instant;
import com.team4.core.auth.AuthDtos.AuthResponse;
import com.team4.core.auth.AuthDtos.UserProfile;
import com.team4.core.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtTokenProvider tokens;
    public JwtService(JwtTokenProvider tokens) { this.tokens = tokens; }
    public long ttlSeconds() { return tokens.getExpirationSeconds(); }
    public AuthResponse issueFaceToken(AdminAccount account, Instant issued, Instant expiry, String sessionId) {
        String token = tokens.generateFaceToken(account.id(), issued, expiry, sessionId);
        return new AuthResponse(token, "Bearer", Math.max(0, Duration.between(Instant.now(), expiry).getSeconds()), UserProfile.from(account));
    }
    public AuthResponse issuePasswordToken(AdminAccount account) {
        Instant now = Instant.now();
        String token = tokens.generatePasswordToken(account.id(), account.role(), now);
        return new AuthResponse(token, "Bearer", ttlSeconds(), UserProfile.from(account));
    }
}
