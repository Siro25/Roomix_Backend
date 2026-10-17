package com.team4.core.events;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalEventMessage {
    private UUID eventId;
    private long sequence;
    private String eventType;
    private UUID roomId;
    private UUID tenancyId;
    private Instant occurredAt;
}
