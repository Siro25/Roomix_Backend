package com.team4.core.events;

import com.team4.core.entities.ProcessedRentalEvent;
import com.team4.core.enums.RoomStatus;
import com.team4.core.repositories.ProcessedRentalEventRepository;
import com.team4.core.repositories.RoomRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalEventProcessor {
    private final RoomRepository rooms;
    private final ProcessedRentalEventRepository processedEvents;
    private final Clock clock;

    @Transactional
    public void process(RentalEventMessage event) {
        if (event.getEventId() == null || event.getRoomId() == null || event.getTenancyId() == null
                || event.getSequence() <= 0 || event.getOccurredAt() == null) {
            throw new IllegalArgumentException("Incomplete rental event");
        }
        // The row lock serializes retries and consumer instances for this room.
        var room = rooms.lockById(event.getRoomId())
                .orElseThrow(() -> new IllegalStateException("Room not found: " + event.getRoomId()));
        if (processedEvents.existsById(event.getEventId())) {
            return;
        }
        var latest = processedEvents.findFirstByRoomIdOrderByEventSequenceDesc(event.getRoomId());
        if (latest.isPresent() && event.getSequence() <= latest.get().getEventSequence()) {
            return;
        }

        switch (event.getEventType()) {
            case "TENANCY_ACTIVATED" -> {
                if (room.getStatus() != RoomStatus.AVAILABLE && room.getStatus() != RoomStatus.OCCUPIED) {
                    throw new IllegalStateException("Cannot activate room in state " + room.getStatus());
                }
                room.setRented(true);
                room.setStatus(RoomStatus.OCCUPIED);
            }
            case "TENANCY_ENDED" -> {
                room.setRented(false);
                if (room.getStatus() == RoomStatus.OCCUPIED) {
                    room.setStatus(RoomStatus.AVAILABLE);
                }
            }
            default -> throw new IllegalArgumentException("Unsupported rental event: " + event.getEventType());
        }
        processedEvents.save(ProcessedRentalEvent.builder()
                .eventId(event.getEventId()).roomId(event.getRoomId())
                .eventSequence(event.getSequence()).eventType(event.getEventType())
                .processedAt(clock.instant()).build());
    }
}
