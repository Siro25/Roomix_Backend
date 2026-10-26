package com.team4.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "password-reset")
@Getter
@Setter
public class PasswordResetProperties {
    private long expirationMinutes;
    private String url;
    private String mailFrom;
}
