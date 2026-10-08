package com.team4.core.services.impl;

import com.team4.core.services.AuthService;
import com.team4.core.dtos.request.LoginRequest;
import com.team4.core.dtos.request.RegisterRequest;
import com.team4.core.dtos.response.AuthResponse;
import com.team4.core.dtos.response.UserProfileResponse;
import com.team4.core.entities.User;
import com.team4.core.enums.Role;
import com.team4.core.enums.UserStatus;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.UserRepository;
import com.team4.core.security.CustomUserDetails;
import com.team4.core.security.JwtTokenProvider;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
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
                .status(UserStatus.ACTIVE)
                .build();

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateException(exception);
        }
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.getUsername(), request.getPassword()));
        return response(((CustomUserDetails) authentication.getPrincipal()).getUser());
    }

    private AuthResponse response(User user) {
        return AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateToken(user))
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationSeconds())
                .user(UserProfileResponse.from(user))
                .build();
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
