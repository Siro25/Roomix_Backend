package com.team4.rental.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLog {
    @Id
    UUID id;

    @Column(name = "user_id")
    UUID userId;

    @Column(nullable = false, length = 60)
    String action;

    @Column(name = "entity_name", nullable = false, length = 50)
    String entityName;

    @Column(name = "entity_id", nullable = false, length = 100)
    String entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", nullable = false, columnDefinition = "jsonb")
    Map<String, Object> oldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", nullable = false, columnDefinition = "jsonb")
    Map<String, Object> newValues;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
}
