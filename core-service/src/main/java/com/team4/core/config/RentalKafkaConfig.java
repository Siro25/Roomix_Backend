package com.team4.core.config;

import java.time.Clock;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class RentalKafkaConfig {
    @Bean
    Clock rentalEventClock() {
        return Clock.systemUTC();
    }

    @Bean
    NewTopic rentalEventsTopic() {
        return new NewTopic("rental-events-topic", 3, (short) 1);
    }

    @Bean
    DefaultErrorHandler rentalEventErrorHandler() {
        // Không bao giờ bỏ qua bản ghi bị lỗi
        return new DefaultErrorHandler(new FixedBackOff(1000L, FixedBackOff.UNLIMITED_ATTEMPTS));
    }
}
