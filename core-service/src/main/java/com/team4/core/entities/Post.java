package com.team4.core.entities;

import com.team4.core.enums.PostStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "posts")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "landlord_id", nullable = false)
    UUID landlordId;

    @Column(name = "room_id", nullable = false)
    UUID roomId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", insertable = false, updatable = false)
    Room room;

    @Setter
    @Column(nullable = false, length = 200)
    String title;

    @Setter
    @Column(nullable = false, columnDefinition = "text")
    String description;

    @Setter
    @Column(name = "rental_price", precision = 15, scale = 2)
    BigDecimal rentalPrice;

    @Setter
    @Column(name = "deposit_amount", nullable = false, precision = 15, scale = 2)
    BigDecimal depositAmount;

    @Setter
    @Column(name = "available_from", nullable = false)
    LocalDate availableFrom;

    @Setter
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    PostStatus status = PostStatus.DRAFT;

    @Setter
    @Column(name = "reject_reason", columnDefinition = "text")
    String rejectReason;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    long viewCount = 0;

    @Column(name = "approved_by")
    UUID approvedBy;

    @Column(name = "approved_at")
    Instant approvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    public void approve(UUID adminId, Instant approvalTime) {
        status = PostStatus.APPROVED;
        approvedBy = adminId;
        approvedAt = approvalTime;
        rejectReason = null;
    }

    public void reject(String reason) {
        status = PostStatus.REJECTED;
        rejectReason = reason;
        approvedBy = null;
        approvedAt = null;
    }
}
