package com.team4.rental.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
@EnableScheduling
public class BusinessTimeConfig {
    @Bean
    Clock businessClock() {
        return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @Bean
    LocalValidatorFactoryBean defaultValidator(Clock businessClock) {
        var validator = new LocalValidatorFactoryBean();
        validator.setConfigurationInitializer(configuration -> configuration.clockProvider(() -> businessClock));
        return validator;
    }
}
