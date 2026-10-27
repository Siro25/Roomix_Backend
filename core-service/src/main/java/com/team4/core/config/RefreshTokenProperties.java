package com.team4.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.refresh-token")
@Getter
@Setter
public class RefreshTokenProperties {
    private long expirationSeconds = 2_592_000;
    private String cookieName = "roomix_refresh";
    private boolean cookieSecure;
    private String cookieSameSite = "Lax";
}
