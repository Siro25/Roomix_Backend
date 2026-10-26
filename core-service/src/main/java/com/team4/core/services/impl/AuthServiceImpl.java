package com.team4.core.services.impl;

import com.team4.core.config.PasswordResetProperties;
import com.team4.core.dtos.request.ForgotPasswordRequest;
import com.team4.core.services.AuthService;
import com.team4.core.dtos.request.LoginRequest;
import com.team4.core.dtos.request.RegisterRequest;
import com.team4.core.dtos.request.ResetPasswordRequest;
import com.team4.core.dtos.response.AuthResponse;
import com.team4.core.dtos.response.UserProfileResponse;
import com.team4.core.entities.User;
import com.team4.core.entities.PasswordResetToken;
import com.team4.core.events.PasswordResetRequestedEvent;
import com.team4.core.enums.Role;
import com.team4.core.enums.UserStatus;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.UserRepository;
import com.team4.core.repositories.PasswordResetTokenRepository;
import com.team4.core.security.CustomUserDetails;
import com.team4.core.security.JwtTokenProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
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
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetProperties passwordResetProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
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

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.getUsername(), request.getPassword()));
        return response(((CustomUserDetails) authentication.getPrincipal()).getUser());
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
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .id(java.util.UUID.randomUUID())
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
                    .digest(request.getToken().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
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
        userRepository.save(user);
        passwordResetTokenRepository.save(token);
    }

    private AuthResponse response(User user) {
        return AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateToken(user))
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationSeconds())
                .user(UserProfileResponse.from(user))
                .build();
    }

    private AuthResponse registrationResponse(User user) {
        if (user.getStatus() == UserStatus.ACTIVE) {
            return response(user);
        }
        return AuthResponse.builder()
                .expiresIn(0)
                .user(UserProfileResponse.from(user))
                .build();
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
