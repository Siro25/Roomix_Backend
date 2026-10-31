package com.team4.core;

import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.team4.core.auth.AdminAccountRepository;
import com.team4.core.security.JwtTokenProvider;
import com.jayway.jsonpath.JsonPath;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="TEST_CORE_DB_URL", matches=".+")
class CoreServiceApplicationTests {
    static final UUID ADMIN = UUID.fromString("10000000-0000-4000-8000-000000000005");
    @Autowired MockMvc http;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.team4.core.services.RoomQueryService roomQueries;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired AdminAccountRepository accounts;
    @Autowired JwtTokenProvider tokens;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry r) {
        String url = System.getenv("TEST_CORE_DB_URL");
        if (url == null || !url.matches("jdbc:postgresql:.*[?&]currentSchema=core_auth_test_[a-f0-9]{32}(&.*)?"))
            throw new IllegalArgumentException("Tests require an isolated core_auth_test_<uuid> schema");
        r.add("spring.datasource.url", () -> url);
        r.add("spring.datasource.username", () -> System.getenv("TEST_CORE_DB_USERNAME"));
        r.add("spring.datasource.password", () -> System.getenv().getOrDefault("TEST_CORE_DB_PASSWORD", ""));
        r.add("security.jwt.secret", () -> java.util.Base64.getEncoder().encodeToString("isolated-test-key-01234567890123456789".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        r.add("security.jwt.issuer", () -> "roomix-core");
        r.add("security.jwt.expiration-seconds", () -> "900");
        r.add("core.auth.allowed-origins", () -> "http://localhost:3000");
        r.add("core.face.service-token", () -> "test-face-service-token-01234567890123456789");
        r.add("core.telegram.bot-token", () -> "");
        r.add("core.telegram.chat-id", () -> "");
        r.add("core.telegram.worker-enabled", () -> "false");
        r.add("spring.kafka.listener.auto-startup", () -> "false");
        r.add("spring.kafka.admin.auto-create", () -> "false");
    }
    @BeforeEach void seed() {
        String schema = jdbc.queryForObject("SELECT current_schema()",String.class);
        if (schema == null || !schema.matches("core_auth_test_[a-f0-9]{32}")) throw new IllegalStateException("Not a test schema");
        jdbc.update("DELETE FROM admin_face_sessions");
        jdbc.update("DELETE FROM users");
        accounts.insertAdmin(ADMIN,"admin_test",passwords.encode("Test-password-123!"),"admin@example.test","Admin Test");
    }
    private String login() throws Exception {
        String body=http.perform(post("/api/auth/login").contentType("application/json")
            .content("{\"username\":\"admin_test\",\"password\":\"Test-password-123!\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body,"$.data.accessToken");
    }
    @Test void passwordLoginUsesTeamJwtAndProfile() throws Exception {
        String token=login();
        var jwt=tokens.decode(token);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwt.getClaimAsString("auth_method")).isEqualTo("PASSWORD");
        assertThat(jwt.getAudience()).contains("roomix-api");
        http.perform(get("/api/users/me").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(ADMIN.toString()))
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }
    @Test void wrongPasswordAndUnknownUserCannotLogin() throws Exception {
        for(String username:new String[]{"admin_test","unknown"})
            http.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\""+username+"\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }
    @Test void lockedAccountCannotLogin() throws Exception {
        jdbc.update("UPDATE users SET status='LOCKED'");
        http.perform(post("/api/auth/login").contentType("application/json")
            .content("{\"username\":\"admin_test\",\"password\":\"Test-password-123!\"}"))
            .andExpect(status().isUnauthorized());
    }
    @Test void tenantPasswordLoginRemainsAvailable() throws Exception {
        jdbc.update("UPDATE users SET role='TENANT'");
        assertThat(tokens.decode(login()).getClaimAsString("role")).isEqualTo("TENANT");
    }
    @Test void teamPublicRoomRouteIsNotBlockedByOldAdminSecurity() throws Exception {
        http.perform(get("/api/rooms/search")).andExpect(status().isOk());
        org.mockito.Mockito.verify(roomQueries).search(org.mockito.ArgumentMatchers.any());
    }
    @Test void configuredFrontendCanPreflight() throws Exception {
        http.perform(options("/api/auth/login").header("Origin","http://localhost:3000")
            .header("Access-Control-Request-Method","POST"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:3000"))
            .andExpect(header().string("Access-Control-Allow-Credentials","true"));
    }

    @Test void passwordLoginKeepsRefreshRotationAndLogout() throws Exception {
        var loggedIn = http.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"admin_test\",\"password\":\"Test-password-123!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse();
        String cookie = loggedIn.getHeader("Set-Cookie");
        assertThat(cookie).contains("roomix_refresh=", "HttpOnly");
        String first = cookie.split(";", 2)[0].split("=", 2)[1];
        var refreshed = http.perform(post("/api/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("roomix_refresh", first)))
                .andExpect(status().isOk()).andReturn().getResponse();
        String jwt = JsonPath.read(refreshed.getContentAsString(), "$.data.accessToken");
        assertThat(tokens.decode(jwt).getClaimAsString("auth_method")).isEqualTo("PASSWORD");
        String rotated = refreshed.getHeader("Set-Cookie").split(";", 2)[0].split("=", 2)[1];
        assertThat(rotated).isNotEqualTo(first);
        http.perform(post("/api/auth/logout").cookie(new jakarta.servlet.http.Cookie("roomix_refresh", rotated)))
                .andExpect(status().isOk());
        http.perform(post("/api/auth/refresh").cookie(new jakarta.servlet.http.Cookie("roomix_refresh", rotated)))
                .andExpect(status().isUnauthorized());
    }

    @Test void mainPasswordRecoveryEndpointsRemainPublic() throws Exception {
        // Unknown email sends no SMTP event; invalid token changes no password.
        http.perform(post("/api/auth/forgot-password").contentType("application/json")
                .content("{\"email\":\"missing@example.test\"}"))
                .andExpect(status().isOk());
        http.perform(post("/api/auth/reset-password").contentType("application/json")
                .content("{\"token\":\"not-a-real-token\",\"newPassword\":\"New-test-password-123!\",\"confirmPassword\":\"New-test-password-123!\"}"))
                .andExpect(status().isBadRequest());
    }
}
