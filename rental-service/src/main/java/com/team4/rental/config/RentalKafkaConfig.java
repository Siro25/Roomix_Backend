package com.team4.rental.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RentalKafkaConfig {
    @Bean
    NewTopic rentalEventsTopic() {
        return new NewTopic("rental-events-topic", 3, (short) 1);
    }
}
