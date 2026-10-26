package com.example.core_service.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JwtConfigurationTests {
    @Test
    void shortSecretsAreRejectedWithoutEchoingTheSecret() {
        assertThatThrownBy(() -> new AdminSecurityConfig().jwtKey("private-short-secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("private-short-secret");
    }

    @Test
    void sufficientlyLongKeyCanBeUsedForHs256() {
        assertThat(new AdminSecurityConfig().jwtKey("01234567890123456789012345678901").getAlgorithm())
                .isEqualTo("HmacSHA256");
    }
}
