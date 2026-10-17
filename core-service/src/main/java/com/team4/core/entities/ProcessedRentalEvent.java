package com.team4.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_rental_events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedRentalEvent {
    @Id
    private UUID eventId;
    @Column(nullable = false)
    private UUID roomId;
    @Column(nullable = false)
    private long eventSequence;
    @Column(nullable = false, length = 40)
    private String eventType;
    @Column(nullable = false)
    private Instant processedAt;
}
