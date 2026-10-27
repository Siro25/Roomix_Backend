package com.team4.rental.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
public class VnpayProperties {
    private String paymentUrl;
    private String tmnCode;
    private String hashSecret;
    private String returnUrl;
    private boolean confirmOnReturn;
}
