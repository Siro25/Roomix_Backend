package com.team4.core;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.team4.core.auth.*;
import com.team4.core.notifications.*;
import com.jayway.jsonpath.JsonPath;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "TEST_CORE_DB_URL", matches = ".+")
class AuthTelegramTests {
    private static final UUID ADMIN = UUID.fromString("10000000-0000-4000-8000-000000000005");
    private static final String PASSWORD = "Private-test-password-123!";
    private static final String INTERNAL = "test-face-service-token-01234567890123456789";
    private static String hash;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc http;
    @Autowired PasswordEncoder passwords;
    @Autowired AdminAccountRepository accounts;
    @Autowired JwtService tokens;
    @Autowired AuthTelegramAlerts alerts;
    @Autowired AuthTelegramRepository queue;
    @Autowired TelegramSettings settings;
    @Autowired TelegramTextWorker worker;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean TelegramTextSender sender;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        CoreServiceApplicationTests.database(registry);
        registry.add("core.telegram.bot-token", () -> "fake-test-token");
        registry.add("core.telegram.chat-id", () -> "123");
        registry.add("core.telegram.worker-enabled", () -> "false");
    }

    @BeforeEach void seed() {
        String schema = jdbc.queryForObject("SELECT current_schema()", String.class);
        if (schema == null || !schema.matches("core_auth_test_[a-f0-9]{32}")) throw new IllegalStateException("Not a test schema");
        jdbc.update("DELETE FROM auth_telegram_outbox");
        jdbc.update("DELETE FROM admin_face_sessions");
        jdbc.update("DELETE FROM users");
        if (hash == null) hash = passwords.encode(PASSWORD);
        accounts.insertAdmin(ADMIN, "admin_test", hash, "admin@example.test", "Admin Test");
        when(sender.send(anyString())).thenReturn(new TelegramTextSender.Result(true, 0));
    }
    private int count() { return jdbc.queryForObject("SELECT count(*) FROM auth_telegram_outbox", Integer.class); }
    private String message() { return jdbc.queryForObject("SELECT message FROM auth_telegram_outbox LIMIT 1", String.class); }
    private String queueStatus() { return jdbc.queryForObject("SELECT status FROM auth_telegram_outbox LIMIT 1", String.class); }
    private String login(String password) throws Exception {
        return http.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"admin_test\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
    }
    private record Ticket(String id, String secret) { }
    private Ticket recognized() throws Exception {
        String created = http.perform(post("/api/auth/face/sessions").contentType("application/json").content("{\"purpose\":\"LOGIN\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Ticket ticket = new Ticket(JsonPath.read(created, "$.authSessionId"), JsonPath.read(created, "$.completionToken"));
        http.perform(post("/internal/face/sessions/validate").header("X-Service-Token", INTERNAL).contentType("application/json")
                .content("{\"authSessionId\":\"" + ticket.id() + "\",\"purpose\":\"LOGIN\"}"))
                .andExpect(status().isOk());
        http.perform(post("/internal/face/results").header("X-Service-Token", INTERNAL).contentType("application/json")
                .content("{\"authSessionId\":\"" + ticket.id() + "\",\"purpose\":\"LOGIN\",\"verified\":true,\"adminId\":\"" + ADMIN + "\",\"reason\":null}"))
                .andExpect(status().isOk());
        return ticket;
    }
    private String complete(Ticket ticket) throws Exception {
        return http.perform(post("/api/auth/face/complete").contentType("application/json")
                .content("{\"authSessionId\":\"" + ticket.id() + "\",\"completionToken\":\"" + ticket.secret() + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test void passwordSuccessEnqueuesWithoutCallingTelegramOnLoginThread() throws Exception {
        String response = login(PASSWORD);
        String jwt = JsonPath.read(response, "$.data.accessToken");
        assertThat(count()).isEqualTo(1);
        assertThat(message()).contains("Đăng nhập mật khẩu thành công", "admin_test").doesNotContain(PASSWORD, jwt);
        verifyNoInteractions(sender);
    }

    @Test void passwordFailureKeepsAlertAfterReturning401() throws Exception {
        http.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"admin_test\",\"password\":\"private-wrong-password\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(count()).isEqualTo(1);
        assertThat(message()).contains("thất bại", "Tài khoản nhập: admin_test").doesNotContain("private-wrong-password");
    }

    @Test void knownTenantPasswordAttemptsDoNotQueueAdminAlerts() throws Exception {
        jdbc.update("UPDATE users SET role = 'TENANT'");
        login(PASSWORD);
        login("wrong-password");
        assertThat(count()).isZero();
        verifyNoInteractions(sender);
    }

    @Test void unknownUsernameFailureIsRecordedWithoutVerifiedIdentity() throws Exception {
        http.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"unknown_admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(count()).isEqualTo(1);
        assertThat(message()).contains("Tài khoản nhập: unknown_admin").doesNotContain("Admin:");
        verifyNoInteractions(sender);
    }

    @Test void faceSuccessOnlyEnqueuesWhenJwtIsIssuedAndDoesNotDuplicateOnRetry() throws Exception {
        Ticket ticket = recognized();
        assertThat(count()).isZero();
        String first = JsonPath.read(complete(ticket), "$.accessToken");
        String second = JsonPath.read(complete(ticket), "$.accessToken");
        assertThat(first).isEqualTo(second);
        assertThat(count()).isEqualTo(1);
        assertThat(message()).contains("Đăng nhập khuôn mặt thành công").doesNotContain(first, ticket.id(), ticket.secret());
    }

    @Test void rejectedFaceCompletionDoesNotSendSuccessAlert() throws Exception {
        Ticket ticket = recognized();
        jdbc.update("UPDATE users SET status = 'LOCKED'");
        http.perform(post("/api/auth/face/complete").contentType("application/json")
                .content("{\"authSessionId\":\"" + ticket.id() + "\",\"completionToken\":\"" + ticket.secret() + "\"}"))
                .andExpect(status().isForbidden());
        assertThat(count()).isZero();
    }

    @Test void deliverySuccessClearsTextAndDoesNotSendAgain() {
        alerts.passwordFailed("admin_test");
        assertThat(worker.deliverNext()).isTrue();
        assertThat(queueStatus()).isEqualTo("SENT");
        assertThat(message()).isNull();
        assertThat(worker.deliverNext()).isFalse();
        verify(sender, times(1)).send(anyString());
    }

    @Test void telegramFailureDoesNotInvalidateAlreadyIssuedJwt() throws Exception {
        String jwt = JsonPath.read(login(PASSWORD), "$.data.accessToken");
        when(sender.send(anyString())).thenReturn(new TelegramTextSender.Result(false, 0));
        worker.deliverNext();
        assertThat(queueStatus()).isEqualTo("PENDING");
        http.perform(get("/api/users/me").header("Authorization", "Bearer " + jwt)).andExpect(status().isOk());
    }

    @Test void rateLimitWaitsForRetryAfterInsteadOfSendingImmediately() {
        alerts.passwordFailed("admin_test");
        when(sender.send(anyString())).thenReturn(new TelegramTextSender.Result(false, 30));
        worker.deliverNext();
        Instant next = jdbc.queryForObject("SELECT available_at FROM auth_telegram_outbox", java.sql.Timestamp.class).toInstant();
        assertThat(next).isAfter(Instant.now().plusSeconds(25));
        assertThat(worker.deliverNext()).isFalse();
        verify(sender, times(1)).send(anyString());
    }

    @Test void fiveFailuresStopRetryingAndClearText() {
        alerts.passwordFailed("admin_test");
        when(sender.send(anyString())).thenReturn(new TelegramTextSender.Result(false, 0));
        for (int attempt = 0; attempt < 5; attempt++) {
            jdbc.update("UPDATE auth_telegram_outbox SET available_at = CURRENT_TIMESTAMP - INTERVAL '1 second'");
            assertThat(worker.deliverNext()).isTrue();
        }
        assertThat(queueStatus()).isEqualTo("FAILED");
        assertThat(message()).isNull();
        assertThat(worker.deliverNext()).isFalse();
        verify(sender, times(5)).send(anyString());
    }

    @Test void expiredTextIsClearedWithoutSending() {
        alerts.passwordFailed("admin_test");
        jdbc.update("UPDATE auth_telegram_outbox SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second'");
        assertThat(worker.deliverNext()).isFalse();
        assertThat(queueStatus()).isEqualTo("FAILED");
        assertThat(message()).isNull();
        verifyNoInteractions(sender);
    }

    @Test void queueWriteRollsBackWithAuthenticationTransaction() {
        assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
            alerts.passwordSucceeded(accounts.findById(ADMIN).orElseThrow());
            throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(count()).isZero();
    }

    @Test void twoWorkersDoNotSendSameRowAtSameTime() throws Exception {
        alerts.passwordFailed("admin_test");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(sender.send(anyString())).thenAnswer(call -> {
            entered.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("test timeout");
            return new TelegramTextSender.Result(true, 0);
        });
        try (var executor = Executors.newSingleThreadExecutor()) {
            var first = executor.submit(worker::deliverNext);
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(worker.deliverNext()).isFalse();
            } finally { release.countDown(); }
            assertThat(first.get(10, TimeUnit.SECONDS)).isTrue();
        }
        verify(sender, times(1)).send(anyString());
    }

    @Test void newWorkerCanDeliverPersistedPendingMessage() {
        alerts.passwordFailed("admin_test");
        TelegramTextWorker restarted = new TelegramTextWorker(settings, queue, sender);
        Boolean delivered = new TransactionTemplate(transactions).execute(transaction -> restarted.deliverNext());
        assertThat(delivered).isTrue();
        assertThat(queueStatus()).isEqualTo("SENT");
    }

    @Test void disabledTelegramDoesNotKeepPendingUsernames() {
        new AuthTelegramAlerts(new TelegramSettings("", ""), queue).passwordFailed("admin_test");
        assertThat(count()).isZero();
    }

    @Test void usernameCannotInsertExtraCaptionLines() {
        alerts.passwordFailed("admin\nFake Success\r\u202etest");
        assertThat(message()).contains("Tài khoản nhập: admin Fake Success  test");
    }
}
