package com.team4.core.entities;

import com.team4.core.enums.ReportStatus;
import com.team4.core.enums.ReportTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
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
@Table(name = "violation_reports")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ViolationReport {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(name = "reporter_id", nullable = false)
    UUID reporterId;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    ReportTargetType targetType;
    @Column(name = "target_id", nullable = false)
    UUID targetId;
    @Column(nullable = false, length = 200)
    String reason;
    @Column(columnDefinition = "text")
    String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    ReportStatus status;
    @Column(columnDefinition = "text")
    String resolution;
    @Column(name = "reviewed_by")
    UUID reviewedBy;
    @Column(name = "reviewed_at")
    Instant reviewedAt;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;
}
