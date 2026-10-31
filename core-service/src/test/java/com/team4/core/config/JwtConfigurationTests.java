package com.team4.core.config;
import org.junit.jupiter.api.Test;
import com.team4.core.security.JwtTokenProvider;
import static org.assertj.core.api.Assertions.*;
class JwtConfigurationTests {
    @Test void rejectsShortDecodedKey() {
        assertThatThrownBy(() -> new JwtTokenProvider("c2hvcnQ=","roomix-core",900,30))
            .isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("c2hvcnQ=");
    }
    @Test void rejectsInvalidBase64() {
        assertThatThrownBy(() -> new JwtTokenProvider("!invalid!","roomix-core",900,30))
            .isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("!invalid!");
    }
}
