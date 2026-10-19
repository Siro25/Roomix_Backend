package com.team4.core.security;

import com.team4.core.entities.User;
import com.team4.core.enums.Role;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider implements JwtDecoder {

    private static final String AUDIENCE = "roomix-api";
    private static final MacAlgorithm ALGORITHM = MacAlgorithm.HS256;

    private final JwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    private final String issuer;

    @Getter
    private final long expirationSeconds;

    public JwtTokenProvider(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.expiration-seconds}") long expirationSeconds,
            @Value("${security.jwt.clock-skew-seconds:30}") long clockSkewSeconds) {
        var key = buildSecretKey(secret);
        this.encoder = NimbusJwtEncoder.withSecretKey(key).algorithm(ALGORITHM).build();
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(ALGORITHM).build();
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;

        validateConfig(issuer, expirationSeconds);
        decoder.setJwtValidator(buildValidator(issuer, clockSkewSeconds));
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .audience(List.of(AUDIENCE))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .id(UUID.randomUUID().toString())
                .claim("role", user.getRole().name())
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(ALGORITHM).build(), claims))
                .getTokenValue();
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        return decoder.decode(token);
    }

    private static SecretKey buildSecretKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("JWT_SECRET must be valid Base64");
        }
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    private static void validateConfig(String issuer, long expirationSeconds) {
        if (issuer.isBlank() || expirationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "JWT requires a non-empty issuer and a positive expiration");
        }
    }

    private static DelegatingOAuth2TokenValidator<Jwt> buildValidator(
            String issuer, long clockSkewSeconds) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ofSeconds(clockSkewSeconds)),
                new JwtIssuerValidator(issuer),
                JwtTokenProvider::validateRequiredClaims);
    }

    private static OAuth2TokenValidatorResult validateRequiredClaims(Jwt jwt) {
        try {
            UUID.fromString(jwt.getSubject());
            Role.valueOf(jwt.getClaimAsString("role"));
            if (jwt.getExpiresAt() == null
                    || jwt.getIssuedAt() == null
                    || !jwt.getAudience().contains(AUDIENCE)) {
                throw new IllegalArgumentException("Missing required claims");
            }
            return OAuth2TokenValidatorResult.success();
        } catch (RuntimeException e) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid access token claims", null));
        }
    }
}
