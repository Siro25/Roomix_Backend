package com.team4.core.services;

import com.team4.core.dtos.response.AuthResponse;

public record AuthSession(AuthResponse response, String refreshToken) {
}
