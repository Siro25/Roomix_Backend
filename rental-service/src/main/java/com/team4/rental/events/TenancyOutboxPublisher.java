package com.team4.rental.events;

import com.team4.rental.entities.TenancyEvent;
import com.team4.rental.repositories.TenancyEventRepository;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenancyOutboxPublisher {
    private final JdbcTemplate jdbcTemplate;
    private final TenancyEventRepository events;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public boolean publishNext() {
        // SKIP LOCKED cho phép nhiều bên publisher cùng làm việc trên các phòng khác
        // nhau. Điều kiện NOT EXISTS giúp ngăn chặn việc một sự kiện phát sinh sau đó
        // đối với cùng một phòng vượt lên trước một sự kiện chưa được gửi đi.
        List<Long> ids = jdbcTemplate.queryForList("""
                SELECT event.sequence FROM tenancy_events event
                WHERE event.published_at IS NULL
                  AND event.next_attempt_at <= CURRENT_TIMESTAMP
                  AND NOT EXISTS (
                      SELECT 1 FROM tenancy_events earlier
                      WHERE earlier.room_id = event.room_id
                        AND earlier.sequence < event.sequence
                        AND earlier.published_at IS NULL
                  )
                ORDER BY event.sequence LIMIT 1 FOR UPDATE OF event SKIP LOCKED
                """, Long.class);
        if (ids.isEmpty()) {
            return false;
        }

        TenancyEvent event = events.findById(ids.getFirst()).orElseThrow();
        RentalEventMessage message = RentalEventMessage.builder()
                .eventId(event.getEventId()).sequence(event.getSequence())
                .eventType(event.getEventType()).roomId(event.getRoomId())
                .tenancyId(event.getTenancyId()).occurredAt(event.getOccurredAt()).build();
        try {
            kafka.send(event.getTopic(), event.getPartitionKey(), objectMapper.writeValueAsString(message))
                    .get(10, TimeUnit.SECONDS);
            event.setPublishedAt(clock.instant());
            event.setLastError(null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Outbox publisher interrupted", ex);
        } catch (Exception ex) {
            event.setAttemptCount(event.getAttemptCount() + 1);
            long delay = Math.min(60, 1L << Math.min(event.getAttemptCount(), 6));
            event.setNextAttemptAt(clock.instant().plusSeconds(delay));
            String error = ex.toString();
            event.setLastError(error.substring(0, Math.min(1000, error.length())));
            log.warn("Kafka publish failed for event {} (attempt {}): {}", event.getEventId(),
                    event.getAttemptCount(), error);
        }
        return true;
    }
}
