package com.team4.core.controllers;

import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.NotificationMessage;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.dtos.response.UnreadNotificationCountResponse;
import com.team4.core.services.NotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationMessage>> getAll(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ApiResponse.<PageResponse<NotificationMessage>>builder()
                .message("Lấy lịch sử thông báo thành công")
                .data(notificationService.getAll(UUID.fromString(jwt.getSubject()), pageable))
                .build();
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadNotificationCountResponse> getUnreadCount(
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<UnreadNotificationCountResponse>builder()
                .message("Lấy số thông báo chưa đọc thành công")
                .data(UnreadNotificationCountResponse.builder()
                        .unreadCount(notificationService.getUnreadCount(
                                UUID.fromString(jwt.getSubject())))
                        .build())
                .build();
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationMessage> markAsRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<NotificationMessage>builder()
                .message("Đánh dấu thông báo đã đọc thành công")
                .data(notificationService.markAsRead(
                        UUID.fromString(jwt.getSubject()), notificationId))
                .build();
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllAsRead(UUID.fromString(jwt.getSubject()));
        return ApiResponse.<Void>builder()
                .message("Đã đánh dấu tất cả thông báo là đã đọc")
                .build();
    }
}
