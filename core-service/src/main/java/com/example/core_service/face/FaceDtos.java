package com.example.core_service.face;

import java.util.UUID;
import jakarta.validation.constraints.*;

public final class FaceDtos {
    private FaceDtos() { }
    public enum Purpose { LOGIN, ENROLL }
    public record CreateRequest(@NotNull Purpose purpose) { }
    public record SessionResponse(String authSessionId, Purpose purpose, double expiresAt, String completionToken) {
        @Override public String toString() { return "SessionResponse[REDACTED]"; }
    }
    public record ValidateRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{16,128}") String authSessionId,
            @NotNull Purpose purpose) { }
    public record ValidateResponse(boolean accepted, UUID adminId, Double expiresAt) { }
    public record ResultRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{16,128}") String authSessionId,
            @NotNull Purpose purpose, @NotNull Boolean verified, UUID adminId,
            @Pattern(regexp = "[A-Z][A-Z0-9_]{0,63}") String reason) { }
    public record Decision(boolean accepted) { }
    public record CompleteRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{16,128}") String authSessionId,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{32,256}") String completionToken) {
        @Override public String toString() { return "CompleteRequest[REDACTED]"; }
    }
}
