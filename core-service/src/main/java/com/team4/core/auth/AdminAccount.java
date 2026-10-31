package com.team4.core.auth;

import java.util.UUID;

// Chỉ dùng nội bộ. Không trả passwordHash ra controller.
public record AdminAccount(UUID id, String username, String passwordHash,
        String email, String fullName, String phoneNumber, String role, String status) {
    public boolean isActiveAdmin() {
        return "ADMIN".equals(role) && "ACTIVE".equals(status);
    }

    @Override
    public String toString() {
        return "AdminAccount[id=" + id + "]";
    }
}
