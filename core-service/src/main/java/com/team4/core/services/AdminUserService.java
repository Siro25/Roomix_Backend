package com.team4.core.services;

import com.team4.core.dtos.request.AuditContext;
import com.team4.core.dtos.response.AdminUserResponse;
import com.team4.core.dtos.response.LandlordPropertyResponse;
import com.team4.core.dtos.response.AdminPageResponse;
import com.team4.core.enums.Role;
import com.team4.core.enums.UserStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    AdminPageResponse<AdminUserResponse> searchUsers(
            Role role, UserStatus status, String keyword, Pageable pageable);

    AdminUserResponse getUser(UUID userId);

    List<LandlordPropertyResponse> getLandlordProperties(UUID landlordId);

    AdminUserResponse lock(UUID userId, AuditContext context);

    AdminUserResponse unlock(UUID userId, AuditContext context);

    AdminUserResponse approveLandlord(UUID userId, AuditContext context);

    AdminUserResponse rejectLandlord(UUID userId, String reason, AuditContext context);
}
