package com.team4.core.controllers;

import com.team4.core.dtos.request.ForgotPasswordRequest;
import com.team4.core.dtos.request.LoginRequest;
import com.team4.core.dtos.request.RegisterRequest;
import com.team4.core.dtos.request.ResetPasswordRequest;
import com.team4.core.dtos.response.AuthResponse;
import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.<AuthResponse>builder()
                .data(response)
                .message("Đăng nhập thành công")
                .build();
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ApiResponse.<AuthResponse>builder()
                .data(response)
                .message("Đăng ký tài khoản thành công")
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
}
