package com.example.core_service.auth;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import com.example.core_service.auth.AuthDtos.AuthResponse;
import com.example.core_service.auth.AuthDtos.UserProfile;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final long ttl;

    public JwtService(JwtEncoder encoder, @Value("${core.auth.jwt-issuer}") String issuer,
            @Value("${core.auth.jwt-ttl-seconds}") long ttl) {
        if (ttl < 60 || ttl > 86400) throw new IllegalArgumentException("JWT TTL must be between 60 and 86400 seconds");
        this.encoder = encoder;
        this.issuer = issuer;
        this.ttl = ttl;
    }

    public long ttlSeconds() { return ttl; }

    public AuthResponse issueFaceToken(AdminAccount account, Instant issuedAt, Instant expiresAt, String sessionId) {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(account.id().toString())
                .id("face:" + sessionId).issuedAt(issuedAt).expiresAt(expiresAt)
                .claim("roles", List.of("ADMIN")).claim("auth_method", "FACE").build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        long remaining = Math.max(0, Duration.between(Instant.now(), expiresAt).getSeconds());
        return new AuthResponse(token, "Bearer", remaining, UserProfile.from(account));
    }

    public AuthResponse issuePasswordToken(AdminAccount account) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(account.id().toString())
                .issuedAt(now).expiresAt(now.plusSeconds(ttl))
                .id(UUID.randomUUID().toString())
                .claim("roles", List.of("ADMIN")).claim("auth_method", "PASSWORD").build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", ttl, UserProfile.from(account));
    }
}
