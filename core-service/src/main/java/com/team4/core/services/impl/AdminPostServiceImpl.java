package com.team4.core.services.impl;

import com.team4.core.dtos.request.AuditContext;
import com.team4.core.dtos.response.AdminPostResponse;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.entities.Post;
import com.team4.core.enums.NotificationType;
import com.team4.core.enums.PostStatus;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.PostRepository;
import com.team4.core.services.AccountNotificationService;
import com.team4.core.services.AdminPostService;
import com.team4.core.services.AuditService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPostServiceImpl implements AdminPostService {
    private static final String POST_ENTITY = "POST";
    private static final String APPROVE_POST = "APPROVE_POST";
    private static final String REJECT_POST = "REJECT_POST";

    private final PostRepository postRepository;
    private final AuditService auditService;
    private final AccountNotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminPostResponse> getPendingPosts(PostStatus status, Pageable pageable) {
        if (status != PostStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_POST_FILTER);
        }
        return PageResponse.from(postRepository.findByStatus(status, pageable), AdminPostResponse::from);
    }

    @Override
    @Transactional
    public AdminPostResponse approve(UUID postId, AuditContext context) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        if (post.getStatus() != PostStatus.PENDING) {
            throw new AppException(ErrorCode.POST_NOT_PENDING);
        }

        Instant approvedAt = Instant.now();
        post.approve(context.getActorId(), approvedAt);
        postRepository.save(post);

        auditService.record(
                APPROVE_POST,
                POST_ENTITY,
                post.getId(),
                Map.of("status", PostStatus.PENDING.name()),
                Map.of(
                        "status", PostStatus.APPROVED.name(),
                        "approvedBy", context.getActorId().toString(),
                        "approvedAt", approvedAt.toString()),
                context);
        notificationService.send(
                post.getLandlordId(),
                post.getId(),
                NotificationType.POST_APPROVAL,
                "POST_APPROVED",
                "Bài đăng đã được duyệt",
                "Bài đăng \"" + post.getTitle() + "\" đã được duyệt công khai.");
        return AdminPostResponse.from(post);
    }

    @Override
    @Transactional
    public AdminPostResponse reject(UUID postId, String reason, AuditContext context) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        if (post.getStatus() != PostStatus.PENDING) {
            throw new AppException(ErrorCode.POST_NOT_PENDING);
        }

        post.reject(reason);
        postRepository.save(post);

        auditService.record(
                REJECT_POST,
                POST_ENTITY,
                post.getId(),
                Map.of("status", PostStatus.PENDING.name()),
                Map.of("status", PostStatus.REJECTED.name(), "rejectReason", reason),
                context);
        notificationService.send(
                post.getLandlordId(),
                post.getId(),
                NotificationType.POST_APPROVAL,
                "POST_REJECTED",
                "Bài đăng chưa được duyệt",
                "Bài đăng \"" + post.getTitle() + "\" bị từ chối. Lý do: " + reason);
        return AdminPostResponse.from(post);
    }
}
