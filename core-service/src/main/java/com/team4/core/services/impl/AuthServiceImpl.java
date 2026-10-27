package com.team4.core.services.impl;

import com.team4.core.config.PasswordResetProperties;
import com.team4.core.config.RefreshTokenProperties;
import com.team4.core.dtos.request.ChangePasswordRequest;
import com.team4.core.dtos.request.ForgotPasswordRequest;
import com.team4.core.dtos.request.LoginRequest;
import com.team4.core.dtos.request.RegisterRequest;
import com.team4.core.dtos.request.ResetPasswordRequest;
import com.team4.core.dtos.response.AuthResponse;
import com.team4.core.dtos.response.UserProfileResponse;
import com.team4.core.entities.PasswordResetToken;
import com.team4.core.entities.RefreshToken;
import com.team4.core.entities.User;
import com.team4.core.enums.Role;
import com.team4.core.enums.UserStatus;
import com.team4.core.events.PasswordResetRequestedEvent;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.PasswordResetTokenRepository;
import com.team4.core.repositories.RefreshTokenRepository;
import com.team4.core.repositories.UserRepository;
import com.team4.core.security.CustomUserDetails;
import com.team4.core.security.JwtTokenProvider;
import com.team4.core.services.AuthService;
import com.team4.core.services.AuthSession;
import com.team4.core.notifications.AuthTelegramAlerts;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTelegramAlerts authAlerts;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetProperties passwordResetProperties;
    private final RefreshTokenProperties refreshTokenProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuthSession register(RegisterRequest request) {
        if (request.getRole() != Role.TENANT && request.getRole() != Role.LANDLORD) {
            throw new AppException(ErrorCode.INVALID_ROLE);
        }
        validateUniqueFields(request);

        var user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .status(initialStatus(request.getRole()))
                .build();

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateException(exception);
        }
        return registrationResponse(user);
    }

    @Transactional
    public AuthSession login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.getUsername(), request.getPassword()));
        } catch (AuthenticationException exception) {
            // A failed username is unverified. Do not attribute a known Tenant/Landlord to Admin.
            String username = request.getUsername().strip().toLowerCase(Locale.ROOT);
            boolean adminOrUnknown = userRepository.findByUsername(username)
                    .map(account -> account.getRole() == Role.ADMIN).orElse(true);
            if (adminOrUnknown) authAlerts.passwordFailed(username);
            throw exception;
        }
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        AuthSession session = response(user);
        if (user.getRole() == Role.ADMIN) authAlerts.passwordSucceeded(user.getId(), user.getUsername());
        return session;
    }

    @Override
    @Transactional(noRollbackFor = AppException.class)
    public AuthSession refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        String tokenHash = hashRefreshToken(rawRefreshToken);
        var stored = refreshTokenRepository.lockByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));
        Instant now = Instant.now();
        if (stored.getUsedAt() != null || stored.getRevokedAt() != null) {
            refreshTokenRepository.revokeFamily(stored.getFamilyId(), now);
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (!stored.getExpiresAt().isAfter(now)) {
            stored.setRevokedAt(now);
            refreshTokenRepository.save(stored);
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (user.getStatus() != UserStatus.ACTIVE) {
            refreshTokenRepository.revokeFamily(stored.getFamilyId(), now);
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        stored.setUsedAt(now);
        refreshTokenRepository.save(stored);
        return createSession(user, stored.getFamilyId());
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.lockByTokenHash(hashRefreshToken(rawRefreshToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String email = request.getEmail().strip().toLowerCase(Locale.ROOT);
        var user = userRepository.findByEmailIgnoreCase(email);
        if (user.isEmpty()) {
            return;
        }
        if (passwordResetProperties.getExpirationMinutes() <= 0) {
            throw new IllegalStateException("PASSWORD_RESET_EXPIRATION_MINUTES must be positive");
        }
        Instant now = Instant.now();
        passwordResetTokenRepository.invalidateActiveByUserId(user.get().getId(), now);
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String tokenHash;
        try {
            tokenHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(user.get().getId())
                .tokenHash(tokenHash)
                .expiresAt(now.plus(Duration.ofMinutes(passwordResetProperties.getExpirationMinutes())))
                .build());
        eventPublisher.publishEvent(new PasswordResetRequestedEvent(
                user.get().getEmail(), user.get().getFullName(), token));
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }
        String tokenHash;
        try {
            tokenHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(request.getToken().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
        PasswordResetToken token = passwordResetTokenRepository.lockByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));
        Instant now = Instant.now();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new AppException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));
        user.changePassword(passwordEncoder.encode(request.getNewPassword()), now);
        token.setUsedAt(now);
        passwordResetTokenRepository.invalidateActiveByUserId(user.getId(), now);
        refreshTokenRepository.revokeAllForUser(user.getId(), now);
        userRepository.save(user);
        passwordResetTokenRepository.save(token);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.CURRENT_PASSWORD_INVALID);
        }
        Instant now = Instant.now();
        user.changePassword(passwordEncoder.encode(request.getNewPassword()), now);
        refreshTokenRepository.revokeAllForUser(userId, now);
        userRepository.save(user);
    }

    private AuthSession response(User user) {
        return createSession(user, UUID.randomUUID());
    }

    private AuthSession createSession(User user, UUID familyId) {
        AuthResponse response = AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateToken(user))
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationSeconds())
                .user(UserProfileResponse.from(user))
                .build();
        String rawToken = generateRefreshToken();
        Instant now = Instant.now();
        refreshTokenRepository.save(RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .familyId(familyId)
                .tokenHash(hashRefreshToken(rawToken))
                .expiresAt(now.plusSeconds(refreshTokenProperties.getExpirationSeconds()))
                .build());
        return new AuthSession(response, rawToken);
    }

    private AuthSession registrationResponse(User user) {
        if (user.getStatus() == UserStatus.ACTIVE) {
            return response(user);
        }
        return new AuthSession(AuthResponse.builder()
                .expiresIn(0)
                .user(UserProfileResponse.from(user))
                .build(), null);
    }

    private String generateRefreshToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashRefreshToken(String rawToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private UserStatus initialStatus(Role role) {
        return role == Role.LANDLORD
                ? UserStatus.PENDING_VERIFICATION
                : UserStatus.ACTIVE;
    }

    private void validateUniqueFields(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_EXISTED);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }
    }

    private AppException duplicateException(DataIntegrityViolationException exception) {
        var message = exception.getMostSpecificCause().getMessage().toLowerCase(Locale.ROOT);
        if (message.contains("username")) {
            return new AppException(ErrorCode.USERNAME_EXISTED);
        }
        return new AppException(ErrorCode.EMAIL_EXISTED);
    }
}
