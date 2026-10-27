package com.team4.core.services;

import com.team4.core.dtos.request.ForgotPasswordRequest;
import com.team4.core.dtos.request.LoginRequest;
import com.team4.core.dtos.request.RegisterRequest;
import com.team4.core.dtos.request.ResetPasswordRequest;
public interface AuthService {
    AuthSession register(RegisterRequest request);
    AuthSession login(LoginRequest request);
    AuthSession refresh(String refreshToken);
    void logout(String refreshToken);
    void requestPasswordReset(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
