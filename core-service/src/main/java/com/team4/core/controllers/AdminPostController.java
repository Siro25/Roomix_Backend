package com.team4.core.controllers;

import com.team4.core.dtos.request.AuditContext;
import com.team4.core.dtos.request.PostRejectionRequest;
import com.team4.core.dtos.response.AdminPostResponse;
import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.enums.PostStatus;
import com.team4.core.services.AdminPostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {
    private final AdminPostService adminPostService;

    @GetMapping
    public ApiResponse<PageResponse<AdminPostResponse>> getPendingPosts(
            @RequestParam(defaultValue = "PENDING") PostStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        PageResponse<AdminPostResponse> response = adminPostService.getPendingPosts(status, pageable);
        return ApiResponse.<PageResponse<AdminPostResponse>>builder()
                .message("Lấy danh sách bài chờ duyệt thành công")
                .data(response)
                .build();
    }

    @PostMapping("/{postId}/approve")
    public ApiResponse<AdminPostResponse> approve(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        AuditContext context = AuditContext.builder()
                .actorId(UUID.fromString(jwt.getSubject()))
                .ipAddress(request.getRemoteAddr())
                .userAgent(request.getHeader("User-Agent"))
                .build();
        AdminPostResponse response = adminPostService.approve(postId, context);
        return ApiResponse.<AdminPostResponse>builder()
                .message("Duyệt bài đăng thành công")
                .data(response)
                .build();
    }

    @PostMapping("/{postId}/reject")
    public ApiResponse<AdminPostResponse> reject(
            @PathVariable UUID postId,
            @Valid @RequestBody PostRejectionRequest body,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        AuditContext context = AuditContext.builder()
                .actorId(UUID.fromString(jwt.getSubject()))
                .ipAddress(request.getRemoteAddr())
                .userAgent(request.getHeader("User-Agent"))
                .build();
        AdminPostResponse response = adminPostService.reject(postId, body.getReason().strip(), context);
        return ApiResponse.<AdminPostResponse>builder()
                .message("Từ chối bài đăng thành công")
                .data(response)
                .build();
    }
}
