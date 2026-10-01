package com.team4.core.services;

import com.team4.core.dtos.request.AuditContext;
import com.team4.core.dtos.response.AdminPostResponse;
import com.team4.core.dtos.response.AdminPageResponse;
import com.team4.core.enums.PostStatus;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface AdminPostService {
    AdminPageResponse<AdminPostResponse> getPendingPosts(PostStatus status, Pageable pageable);

    AdminPostResponse approve(UUID postId, AuditContext context);

    AdminPostResponse reject(UUID postId, String reason, AuditContext context);
}
