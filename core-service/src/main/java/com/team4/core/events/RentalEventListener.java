package com.team4.core.events;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RentalEventListener {
    private final RentalEventProcessor processor;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "rental-events-topic")
    public void onEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        RentalEventMessage event = objectMapper.readValue(record.value(), RentalEventMessage.class);
        if (!UUID.fromString(record.key()).equals(event.getRoomId())) {
            throw new IllegalArgumentException("Kafka key does not match roomId");
        }
        processor.process(event);
        acknowledgment.acknowledge();
    }
}
