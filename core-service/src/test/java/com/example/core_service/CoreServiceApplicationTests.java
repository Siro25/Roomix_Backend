package com.example.core_service;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.example.core_service.auth.AdminAccountRepository;
import com.example.core_service.auth.BootstrapAdmin;
import com.jayway.jsonpath.JsonPath;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "TEST_CORE_DB_URL", matches = ".+")
class CoreServiceApplicationTests {
    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-4000-8000-000000000005");
    private static final String PASSWORD = "Test-password-only-123!";
    private static String hash;
    @Autowired MockMvc http;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtDecoder decoder;
    @Autowired JwtEncoder encoder;
    @Autowired AdminAccountRepository accounts;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("TEST_CORE_DB_URL");
        if (url == null || !url.matches("jdbc:postgresql:.*[?&]currentSchema=core_auth_test_[a-f0-9]{32}(&.*)?")) {
            throw new IllegalArgumentException("Tests require an isolated core_auth_test_<uuid> schema");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> System.getenv("TEST_CORE_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("TEST_CORE_DB_PASSWORD", ""));
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("core.auth.jwt-secret", () -> "only-for-isolated-tests-012345678901234567890123456789");
        registry.add("core.auth.jwt-issuer", () -> "roomix-core");
        registry.add("core.auth.jwt-ttl-seconds", () -> "900");
        registry.add("core.auth.bootstrap.enabled", () -> "false");
        registry.add("core.auth.allowed-origins", () -> "http://localhost:3000");
        registry.add("core.face.service-token", () -> "test-face-service-token-01234567890123456789");
        registry.add("core.telegram.bot-token", () -> "");
        registry.add("core.telegram.chat-id", () -> "");
        registry.add("core.telegram.worker-enabled", () -> "false");
    }

    @BeforeEach
    void seed() {
        String schema = jdbc.queryForObject("SELECT current_schema()", String.class);
        if (schema == null || !schema.matches("core_auth_test_[a-f0-9]{32}")) {
            throw new IllegalStateException("Refusing to change a non-test schema");
        }
        jdbc.update("DELETE FROM users");
        if (hash == null) hash = passwords.encode(PASSWORD);
        accounts.insertAdmin(ADMIN_ID, "admin_test", hash, "admin@example.test", "Admin Test");
    }

    private String login() throws Exception {
        String response = http.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"admin_test\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private String signed(JwtClaimsSet claims) {
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    void passwordLoginIssuesRealJwtAndProfileWithoutFaceService() throws Exception {
        String token = login();
        Jwt jwt = decoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(ADMIN_ID.toString());
        assertThat(jwt.getClaimAsString("auth_method")).isEqualTo("PASSWORD");
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
        http.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.id").value(ADMIN_ID.toString())).andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.status").value("ACTIVE")).andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void configuredFrontendCanPreflightLoginWithoutJwt() throws Exception {
        http.perform(options("/api/auth/login").header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void unconfiguredOriginCannotUseCors() throws Exception {
        http.perform(options("/api/auth/login").header("Origin", "http://untrusted.test")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordAndUnknownUsernameReturnSameGenericError() throws Exception {
        for (String username : new String[] {"admin_test", "does_not_exist"}) {
            http.perform(post("/api/auth/login").contentType("application/json")
                    .content("{\"username\":\"" + username + "\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.accessToken").doesNotExist());
        }
    }

    @Test
    void lockedOrNonAdminCannotLogin() throws Exception {
        for (String update : new String[] {"status = 'LOCKED'", "status = 'ACTIVE', role = 'TENANT'"}) {
            jdbc.update("UPDATE users SET " + update);
            http.perform(post("/api/auth/login").contentType("application/json")
                    .content("{\"username\":\"admin_test\",\"password\":\"" + PASSWORD + "\"}"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.accessToken").doesNotExist());
        }
    }

    @Test
    void profileRequiresValidSignature() throws Exception {
        http.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        http.perform(get("/api/users/me").header("Authorization", "Bearer mock-only-demo"))
                .andExpect(status().isUnauthorized());
        String token = login();
        int signature = token.lastIndexOf('.') + 1;
        String changed = token.substring(0, signature) + (token.charAt(signature) == 'A' ? 'B' : 'A') + token.substring(signature + 1);
        http.perform(get("/api/users/me").header("Authorization", "Bearer " + changed))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredWrongIssuerAndMissingExpiryTokensAreRejected() throws Exception {
        JwtClaimsSet[] invalid = {
            JwtClaimsSet.builder().issuer("roomix-core").subject(ADMIN_ID.toString()).expiresAt(Instant.now().minusSeconds(600)).build(),
            JwtClaimsSet.builder().issuer("another-core").subject(ADMIN_ID.toString()).expiresAt(Instant.now().plusSeconds(900)).build(),
            JwtClaimsSet.builder().issuer("roomix-core").subject(ADMIN_ID.toString()).build(),
            JwtClaimsSet.builder().issuer("roomix-core").expiresAt(Instant.now().plusSeconds(900)).build()
        };
        for (JwtClaimsSet claims : invalid) {
            http.perform(get("/api/users/me").header("Authorization", "Bearer " + signed(claims)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void lockAfterLoginRevokesProfileAccessEvenWhileJwtIsValid() throws Exception {
        String token = login();
        jdbc.update("UPDATE users SET status = 'LOCKED'");
        http.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void roleChangeAfterLoginRevokesProfileAccess() throws Exception {
        String token = login();
        jdbc.update("UPDATE users SET role = 'TENANT'");
        http.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletedAccountCannotUseOldJwt() throws Exception {
        String token = login();
        jdbc.update("DELETE FROM users");
        http.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedRequestsAreRejectedWithoutEchoingCredentials() throws Exception {
        for (String body : new String[] {"{}", "{", "{\"username\":\" \u0020\",\"password\":\"private\"}",
                "{\"username\":\"admin_test\",\"password\":\"" + "a".repeat(73) + "\"}"}) {
            http.perform(post("/api/auth/login").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }
    }

    @Test
    void utf8PasswordIsNotSilentlyTruncatedByBcrypt() throws Exception {
        http.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"admin_test\",\"password\":\"" + "é".repeat(40) + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedSignedSubjectIsUnauthorizedInsteadOfInternalError() throws Exception {
        String token = signed(JwtClaimsSet.builder().issuer("roomix-core").subject("not-a-uuid")
                .expiresAt(Instant.now().plusSeconds(900)).build());
        http.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bootstrapCreatesBcryptAccountButNeverOverwritesAnExistingOne() throws Exception {
        BootstrapAdmin bootstrap = new BootstrapAdmin(accounts, passwords, "second_admin", PASSWORD,
                "second@example.test", "Second Admin");
        bootstrap.run(new DefaultApplicationArguments(new String[0]));
        var account = accounts.findByUsername("second_admin").orElseThrow();
        assertThat(account.isActiveAdmin()).isTrue();
        assertThat(account.passwordHash()).isNotEqualTo(PASSWORD);
        assertThat(passwords.matches(PASSWORD, account.passwordHash())).isTrue();
        assertThatThrownBy(() -> bootstrap.run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class);
        assertThat(accounts.findByUsername("second_admin").orElseThrow().passwordHash()).isEqualTo(account.passwordHash());
    }
}
