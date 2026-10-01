package com.team4.core.services.impl;

import com.team4.core.dtos.request.AuditContext;
import com.team4.core.dtos.response.AdminUserResponse;
import com.team4.core.dtos.response.LandlordPropertyResponse;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.entities.User;
import com.team4.core.enums.Role;
import com.team4.core.enums.UserStatus;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.HouseRepository;
import com.team4.core.repositories.UserRepository;
import com.team4.core.services.AccountNotificationService;
import com.team4.core.services.AdminUserService;
import com.team4.core.services.AuditService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {
    private static final String LOCK_USER = "LOCK_USER";
    private static final String UNLOCK_USER = "UNLOCK_USER";
    private static final String APPROVE_LANDLORD = "APPROVE_LANDLORD";
    private static final String REJECT_LANDLORD = "REJECT_LANDLORD";

    private final UserRepository userRepository;
    private final HouseRepository houseRepository;
    private final AuditService auditService;
    private final AccountNotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> searchUsers(
            Role role, UserStatus status, String keyword, Pageable pageable) {
        validateManagedRole(role);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.strip() : null;
        var users = userRepository.searchAdminUsers(role, status, normalizedKeyword, pageable);
        return PageResponse.from(users, AdminUserResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUser(UUID userId) {
        return AdminUserResponse.from(findManagedUser(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LandlordPropertyResponse> getLandlordProperties(UUID landlordId) {
        User landlord = findManagedUser(landlordId);
        requireLandlord(landlord);
        return houseRepository.findDistinctByLandlordIdOrderByCreatedAtDesc(landlordId).stream()
                .map(LandlordPropertyResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public AdminUserResponse lock(UUID userId, AuditContext context) {
        User user = findManagedUser(userId);
        requireTransition(user, UserStatus.ACTIVE, UserStatus.LOCKED);
        return changeStatus(
                user,
                UserStatus.LOCKED,
                LOCK_USER,
                "ACCOUNT_LOCKED",
                "Tài khoản đã bị khóa",
                "Tài khoản của bạn đã bị quản trị viên khóa.",
                context);
    }

    @Override
    @Transactional
    public AdminUserResponse unlock(UUID userId, AuditContext context) {
        User user = findManagedUser(userId);
        requireTransition(user, UserStatus.LOCKED, UserStatus.ACTIVE);
        return changeStatus(
                user,
                UserStatus.ACTIVE,
                UNLOCK_USER,
                "ACCOUNT_UNLOCKED",
                "Tài khoản đã được mở khóa",
                "Tài khoản của bạn đã được quản trị viên mở khóa.",
                context);
    }

    @Override
    @Transactional
    public AdminUserResponse approveLandlord(UUID userId, AuditContext context) {
        User landlord = findPendingLandlord(userId);
        return changeStatus(
                landlord,
                UserStatus.ACTIVE,
                APPROVE_LANDLORD,
                "LANDLORD_APPROVED",
                "Hồ sơ chủ trọ đã được duyệt",
                "Hồ sơ chủ trọ của bạn đã được quản trị viên phê duyệt.",
                context);
    }

    @Override
    @Transactional
    public AdminUserResponse rejectLandlord(UUID userId, String reason, AuditContext context) {
        User landlord = findPendingLandlord(userId);
        auditService.record(
                REJECT_LANDLORD,
                landlord.getId(),
                statusValues(landlord.getStatus()),
                Map.of("status", landlord.getStatus().name(), "decision", "REJECTED", "reason", reason),
                context);
        notificationService.send(
                landlord.getId(),
                "LANDLORD_REJECTED",
                "Hồ sơ chủ trọ chưa được duyệt",
                reason);
        return AdminUserResponse.from(landlord);
    }

    private AdminUserResponse changeStatus(
            User user,
            UserStatus newStatus,
            String action,
            String event,
            String title,
            String content,
            AuditContext context) {
        UserStatus oldStatus = user.getStatus();
        user.changeStatus(newStatus);
        userRepository.save(user);

        auditService.record(
                action,
                user.getId(),
                statusValues(oldStatus),
                statusValues(newStatus),
                context);
        notificationService.send(user.getId(), event, title, content);
        return AdminUserResponse.from(user);
    }

    private User findManagedUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        validateManagedRole(user.getRole());
        return user;
    }

    private User findPendingLandlord(UUID userId) {
        User user = findManagedUser(userId);
        requireLandlord(user);
        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new AppException(ErrorCode.LANDLORD_NOT_PENDING);
        }
        return user;
    }

    private void validateManagedRole(Role role) {
        if (role == null || role == Role.ADMIN) {
            throw new AppException(role == Role.ADMIN
                    ? ErrorCode.ADMIN_ACCOUNT_MANAGEMENT_NOT_ALLOWED
                    : ErrorCode.INVALID_USER_FILTER);
        }
    }

    private void requireLandlord(User user) {
        if (user.getRole() != Role.LANDLORD) {
            throw new AppException(ErrorCode.LANDLORD_REQUIRED);
        }
    }

    private void requireTransition(User user, UserStatus expectedStatus, UserStatus targetStatus) {
        if (user.getStatus() == targetStatus) {
            throw new AppException(ErrorCode.USER_STATUS_UNCHANGED);
        }
        if (user.getStatus() != expectedStatus) {
            throw new AppException(ErrorCode.INVALID_USER_STATUS_TRANSITION);
        }
    }

    private Map<String, Object> statusValues(UserStatus status) {
        return Map.of("status", status.name());
    }
}
