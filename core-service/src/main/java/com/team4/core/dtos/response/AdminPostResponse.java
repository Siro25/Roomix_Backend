package com.team4.core.dtos.response;

import com.team4.core.entities.Post;
import com.team4.core.enums.PostStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminPostResponse {
    UUID id;
    UUID landlordId;
    UUID roomId;
    String title;
    String description;
    BigDecimal rentalPrice;
    BigDecimal depositAmount;
    LocalDate availableFrom;
    PostStatus status;
    String rejectReason;
    long viewCount;
    UUID approvedBy;
    Instant approvedAt;
    Instant createdAt;
    Instant updatedAt;

    public static AdminPostResponse from(Post post) {
        return AdminPostResponse.builder()
                .id(post.getId())
                .landlordId(post.getLandlordId())
                .roomId(post.getRoomId())
                .title(post.getTitle())
                .description(post.getDescription())
                .rentalPrice(post.getRentalPrice())
                .depositAmount(post.getDepositAmount())
                .availableFrom(post.getAvailableFrom())
                .status(post.getStatus())
                .rejectReason(post.getRejectReason())
                .viewCount(post.getViewCount())
                .approvedBy(post.getApprovedBy())
                .approvedAt(post.getApprovedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
