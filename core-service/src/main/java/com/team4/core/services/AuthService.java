package com.team4.core.services;

import com.team4.core.dtos.request.ForgotPasswordRequest;
import com.team4.core.dtos.request.LoginRequest;
import com.team4.core.dtos.request.RegisterRequest;
import com.team4.core.dtos.request.ResetPasswordRequest;
import com.team4.core.dtos.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void requestPasswordReset(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
