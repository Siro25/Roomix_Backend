package com.team4.rental.entities;

import com.team4.rental.enums.TenancyEventType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "tenancy_events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenancyEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sequence;
    @Column(nullable = false, unique = true)
    private UUID eventId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TenancyEventType eventType;
    @Column(nullable = false)
    private UUID roomId;
    @Column(nullable = false)
    private UUID tenancyId;
    @Column(nullable = false, length = 36)
    private String partitionKey;
    @Column(nullable = false, length = 100)
    private String topic;
    @Column(nullable = false)
    private Instant occurredAt;
    @Setter
    private Instant publishedAt;
    @Setter
    @Column(nullable = false)
    private int attemptCount;
    @Setter
    @Builder.Default
    @Column(nullable = false)
    private Instant nextAttemptAt = Instant.EPOCH;
    @Setter
    @Column(length = 1000)
    private String lastError;
}
