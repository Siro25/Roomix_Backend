package com.team4.core.controllers;

import com.team4.core.dtos.request.AuditContext;
import com.team4.core.dtos.request.LandlordRejectionRequest;
import com.team4.core.dtos.response.AdminUserResponse;
import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.LandlordPropertyResponse;
import com.team4.core.dtos.response.AdminPageResponse;
import com.team4.core.enums.Role;
import com.team4.core.enums.UserStatus;
import com.team4.core.services.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminUserService adminUserService;

    @GetMapping
    public ApiResponse<AdminPageResponse<AdminUserResponse>> search(
            @RequestParam Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        AdminPageResponse<AdminUserResponse> response = adminUserService.searchUsers(role, status, keyword, pageable);
        return ApiResponse.<AdminPageResponse<AdminUserResponse>>builder()
                .data(response)
                .message("Thành công")
                .build();
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserResponse> getUser(@PathVariable UUID userId) {
        AdminUserResponse response = adminUserService.getUser(userId);
        return ApiResponse.<AdminUserResponse>builder()
                .data(response)
                .message("Thành công")
                .build();
    }

    @GetMapping("/{userId}/properties")
    public ApiResponse<List<LandlordPropertyResponse>> getProperties(@PathVariable UUID userId) {
        List<LandlordPropertyResponse> response = adminUserService.getLandlordProperties(userId);
        return ApiResponse.<List<LandlordPropertyResponse>>builder()
                .data(response)
                .message("Thành công")
                .build();
    }

    @PostMapping("/{userId}/lock")
    public ApiResponse<AdminUserResponse> lock(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        AuditContext context = AuditContext.builder()
                .actorId(UUID.fromString(jwt.getSubject()))
                .userAgent(request.getHeader("User-Agent"))
                .build();
        AdminUserResponse response = adminUserService.lock(userId, context);
        return ApiResponse.<AdminUserResponse>builder()
                .data(response)
                .message("Khóa tài khoản thành công")
                .build();
    }

    @PostMapping("/{userId}/unlock")
    public ApiResponse<AdminUserResponse> unlock(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        AuditContext context = AuditContext.builder()
                .actorId(UUID.fromString(jwt.getSubject()))
                .userAgent(request.getHeader("User-Agent"))
                .build();
        AdminUserResponse response = adminUserService.unlock(userId, context);
        return ApiResponse.<AdminUserResponse>builder()
                .data(response)
                .message("Mở khóa tài khoản thành công")
                .build();
    }

    @PostMapping("/{userId}/approve-landlord")
    public ApiResponse<AdminUserResponse> approveLandlord(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        AuditContext context = AuditContext.builder()
                .actorId(UUID.fromString(jwt.getSubject()))
                .userAgent(request.getHeader("User-Agent"))
                .build();
        AdminUserResponse response = adminUserService.approveLandlord(userId, context);
        return ApiResponse.<AdminUserResponse>builder()
                .data(response)
                .message("Duyệt hồ sơ chủ trọ thành công")
                .build();
    }

    @PostMapping("/{userId}/reject-landlord")
    public ApiResponse<AdminUserResponse> rejectLandlord(
            @PathVariable UUID userId,
            @Valid @RequestBody LandlordRejectionRequest body,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        AuditContext context = AuditContext.builder()
                .actorId(UUID.fromString(jwt.getSubject()))
                .userAgent(request.getHeader("User-Agent"))
                .build();
        AdminUserResponse response = adminUserService.rejectLandlord(userId, body.getReason().strip(), context);
        return ApiResponse.<AdminUserResponse>builder()
                .data(response)
                .message("Đã từ chối hồ sơ chủ trọ")
                .build();
    }
}
