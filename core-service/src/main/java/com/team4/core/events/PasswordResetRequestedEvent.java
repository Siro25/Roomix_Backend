package com.team4.core.events;

public record PasswordResetRequestedEvent(String email, String fullName, String token) {
}
