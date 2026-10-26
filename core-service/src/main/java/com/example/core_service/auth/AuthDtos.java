package com.example.core_service.auth;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() { }

    public record LoginRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(max = 72) String password) {
        @Override
        public String toString() { return "LoginRequest[REDACTED]"; }
    }

    public record UserProfile(UUID id, String username, String email, String fullName,
            String phoneNumber, String role, String status) {
        public static UserProfile from(AdminAccount account) {
            return new UserProfile(account.id(), account.username(), account.email(), account.fullName(),
                    account.phoneNumber(), account.role(), account.status());
        }
    }

    public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserProfile user) { }
}
