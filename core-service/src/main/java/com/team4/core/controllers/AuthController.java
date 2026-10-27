package com.team4.core.controllers;

import com.team4.core.config.RefreshTokenProperties;
import com.team4.core.dtos.request.ChangePasswordRequest;
import com.team4.core.dtos.request.ForgotPasswordRequest;
import com.team4.core.dtos.request.LoginRequest;
import com.team4.core.dtos.request.RegisterRequest;
import com.team4.core.dtos.request.ResetPasswordRequest;
import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.AuthResponse;
import com.team4.core.services.AuthService;
import com.team4.core.services.AuthSession;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenProperties refreshTokenProperties;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthSession session = authService.login(request);
        writeRefreshCookie(response, session.refreshToken());

        return ApiResponse.<AuthResponse>builder()
                .data(session.response())
                .message("Đăng nhập thành công")
                .build();
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthSession session = authService.register(request);
        writeRefreshCookie(response, session.refreshToken());

        return ApiResponse.<AuthResponse>builder()
                .data(session.response())
                .message("Đăng ký tài khoản thành công")
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(
            @CookieValue(name = "${security.refresh-token.cookie-name:roomix_refresh}", required = false) String refreshToken,
            HttpServletResponse response) {
        AuthSession session = authService.refresh(refreshToken);
        writeRefreshCookie(response, session.refreshToken());

        return ApiResponse.<AuthResponse>builder()
                .data(session.response())
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(name = "${security.refresh-token.cookie-name:roomix_refresh}", required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        clearRefreshCookie(response);

        return ApiResponse.<Void>builder()
                .message("Đăng xuất thành công")
                .build();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
        return ApiResponse.<Void>builder()
                .message("Nếu email tồn tại, hướng dẫn đặt lại mật khẩu đã được gửi")
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .message("Đặt lại mật khẩu thành công, vui lòng đăng nhập lại")
                .build();
    }

    private void writeRefreshCookie(HttpServletResponse response, String value) {
        if (value == null) {
            clearRefreshCookie(response);
            return;
        }
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(value,
                refreshTokenProperties.getExpirationSeconds()).toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString());
    }

    private ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(refreshTokenProperties.getCookieName(), value)
                .httpOnly(true)
                .secure(refreshTokenProperties.isCookieSecure())
                .sameSite(refreshTokenProperties.getCookieSameSite())
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        authService.changePassword(UUID.fromString(jwt.getSubject()), request);
        return ApiResponse.<Void>builder()
                .message("Đổi mật khẩu thành công, vui lòng đăng nhập lại")
                .build();
    }
}
