package com.team4.core;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import com.team4.core.auth.*;
import com.jayway.jsonpath.JsonPath;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "TEST_CORE_DB_URL", matches = ".+")
class FaceSessionTests {
    private static final UUID FIRST = UUID.fromString("10000000-0000-4000-8000-000000000005");
    private static final UUID SECOND = UUID.fromString("10000000-0000-4000-8000-000000000006");
    private static final String SERVICE_TOKEN = "test-face-service-token-01234567890123456789";
    private static String passwordHash;
    @Autowired MockMvc http;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired AdminAccountRepository accounts;
    @Autowired JwtService tokens;
    @Autowired org.springframework.security.oauth2.jwt.JwtDecoder decoder;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) { CoreServiceApplicationTests.database(registry); }

    @BeforeEach
    void seed() {
        String schema = jdbc.queryForObject("SELECT current_schema()", String.class);
        if (schema == null || !schema.matches("core_auth_test_[a-f0-9]{32}")) throw new IllegalStateException("Not a test schema");
        jdbc.update("DELETE FROM admin_face_sessions");
        jdbc.update("DELETE FROM users");
        if (passwordHash == null) passwordHash = passwords.encode("Test-only-password-123!");
        accounts.insertAdmin(FIRST, "first_admin", passwordHash, "first@example.test", "First Admin");
        accounts.insertAdmin(SECOND, "second_admin", passwordHash, "second@example.test", "Second Admin");
    }

    private record Ticket(String id, String secret, String purpose) { }
    private String passwordToken(UUID id) { return tokens.issuePasswordToken(accounts.findById(id).orElseThrow()).accessToken(); }
    private Ticket create(String purpose, String jwt) throws Exception {
        var request = post("/api/auth/face/sessions").contentType("application/json").content("{\"purpose\":\"" + purpose + "\"}");
        if (jwt != null) request.header("Authorization", "Bearer " + jwt);
        String body = http.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new Ticket(JsonPath.read(body, "$.authSessionId"), JsonPath.read(body, "$.completionToken"), purpose);
    }
    private ResultActions validate(Ticket ticket, String jwt) throws Exception {
        var request = post("/internal/face/sessions/validate").header("X-Service-Token", SERVICE_TOKEN)
                .contentType("application/json").content("{\"authSessionId\":\"" + ticket.id() + "\",\"purpose\":\"" + ticket.purpose() + "\"}");
        if (jwt != null) request.header("Authorization", "Bearer " + jwt);
        return http.perform(request);
    }
    private ResultActions result(Ticket ticket, boolean verified, UUID admin, String reason) throws Exception {
        String body = "{\"authSessionId\":\"" + ticket.id() + "\",\"purpose\":\"" + ticket.purpose()
                + "\",\"verified\":" + verified + ",\"adminId\":" + (admin == null ? "null" : "\"" + admin + "\"")
                + ",\"reason\":" + (reason == null ? "null" : "\"" + reason + "\"") + "}";
        return http.perform(post("/internal/face/results").header("X-Service-Token", SERVICE_TOKEN)
                .contentType("application/json").content(body));
    }
    private ResultActions complete(Ticket ticket) throws Exception {
        return http.perform(post("/api/auth/face/complete").contentType("application/json")
                .content("{\"authSessionId\":\"" + ticket.id() + "\",\"completionToken\":\"" + ticket.secret() + "\"}"));
    }
    private Ticket recognized() throws Exception {
        Ticket ticket = create("LOGIN", null);
        validate(ticket, null).andExpect(jsonPath("$.accepted").value(true));
        result(ticket, true, FIRST, null).andExpect(jsonPath("$.accepted").value(true));
        return ticket;
    }
    private String token(ResultActions response) throws Exception {
        return JsonPath.read(response.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    @Test void anonymousLoginCreatesUniqueSessionsAndOnlyStoresSecretHash() throws Exception {
        Ticket first = create("LOGIN", null);
        Ticket second = create("LOGIN", null);
        assertThat(first.id()).hasSize(43).isNotEqualTo(second.id());
        assertThat(first.secret()).hasSize(43).isNotEqualTo(second.secret());
        String stored = jdbc.queryForObject("SELECT completion_hash FROM admin_face_sessions WHERE auth_session_id = ?", String.class, first.id());
        assertThat(stored).hasSize(64).isNotEqualTo(first.secret());
    }

    @Test void loginFlowIssuesFaceJwtAndRepeatingCompleteReturnsSameToken() throws Exception {
        Ticket ticket = recognized();
        String first = token(complete(ticket));
        String second = token(complete(ticket));
        assertThat(first).isEqualTo(second);
        var jwt = decoder.decode(first);
        assertThat(jwt.getSubject()).isEqualTo(FIRST.toString());
        assertThat(jwt.getClaimAsString("auth_method")).isEqualTo("FACE");
        assertThat(jwt.getId()).isEqualTo("face:" + ticket.id());
        http.perform(get("/api/users/me").header("Authorization", "Bearer " + first)).andExpect(status().isOk());
    }

    @Test void enrollmentIdentityComesFromPasswordJwtAndDoesNotGetCompletionSecret() throws Exception {
        String jwt = passwordToken(FIRST);
        Ticket ticket = create("ENROLL", jwt);
        assertThat(ticket.secret()).isNull();
        validate(ticket, jwt).andExpect(status().isOk()).andExpect(jsonPath("$.adminId").value(FIRST.toString()))
                .andExpect(jsonPath("$.completionToken").doesNotExist()).andExpect(jsonPath("$.accessToken").doesNotExist());
        result(ticket, true, FIRST, null).andExpect(status().isOk()).andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test void enrollmentRequiresJwt() throws Exception {
        http.perform(post("/api/auth/face/sessions").contentType("application/json").content("{\"purpose\":\"ENROLL\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void faceJwtCannotCreateEnrollmentSession() throws Exception {
        String jwt = token(complete(recognized()));
        http.perform(post("/api/auth/face/sessions").header("Authorization", "Bearer " + jwt)
                .contentType("application/json").content("{\"purpose\":\"ENROLL\"}"))
                .andExpect(status().isForbidden());
    }

    @Test void anotherAdminCannotValidateEnrollmentSession() throws Exception {
        Ticket ticket = create("ENROLL", passwordToken(FIRST));
        validate(ticket, passwordToken(SECOND)).andExpect(status().isOk()).andExpect(jsonPath("$.accepted").value(false));
    }

    @Test void sameAdminWithDifferentJwtCannotTakeOverEnrollmentSession() throws Exception {
        String firstJwt = passwordToken(FIRST);
        Ticket ticket = create("ENROLL", firstJwt);
        String anotherJwt = passwordToken(FIRST);
        assertThat(anotherJwt).isNotEqualTo(firstJwt);
        validate(ticket, anotherJwt).andExpect(jsonPath("$.accepted").value(false));
        validate(ticket, firstJwt).andExpect(jsonPath("$.accepted").value(true));
    }

    @Test void internalEndpointsRequireServiceTokenEvenWithAdminJwt() throws Exception {
        Ticket ticket = create("LOGIN", null);
        for (String supplied : new String[] {"", "wrong-token"}) {
            http.perform(post("/internal/face/sessions/validate").header("X-Service-Token", supplied)
                    .header("Authorization", "Bearer " + passwordToken(FIRST)).contentType("application/json")
                    .content("{\"authSessionId\":\"" + ticket.id() + "\",\"purpose\":\"LOGIN\"}"))
                    .andExpect(status().isUnauthorized());
            http.perform(post("/internal/face/results").header("X-Service-Token", supplied)
                    .contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test void callbackBeforeValidationCannotVerifyLogin() throws Exception {
        Ticket ticket = create("LOGIN", null);
        result(ticket, true, FIRST, null).andExpect(status().isConflict());
        complete(ticket).andExpect(status().isConflict());
    }

    @Test void purposeCannotBeChangedAtValidationOrCallback() throws Exception {
        Ticket ticket = create("LOGIN", null);
        Ticket changed = new Ticket(ticket.id(), ticket.secret(), "ENROLL");
        validate(changed, passwordToken(FIRST)).andExpect(status().isForbidden());
        result(changed, true, FIRST, null).andExpect(status().isForbidden());
    }

    @Test void expiredSessionCannotValidateReceiveResultOrComplete() throws Exception {
        Ticket ticket = create("LOGIN", null);
        jdbc.update("UPDATE admin_face_sessions SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second'");
        validate(ticket, null).andExpect(status().isGone());
        result(ticket, true, FIRST, null).andExpect(status().isGone());
        complete(ticket).andExpect(status().isGone());
    }

    @Test void successfulResultNeedsAdminIdAndFailureCannotCarryOne() throws Exception {
        Ticket ticket = create("LOGIN", null);
        validate(ticket, null);
        result(ticket, true, null, null).andExpect(status().isBadRequest());
        result(ticket, true, FIRST, "NO_FACE").andExpect(status().isBadRequest());
        result(ticket, false, FIRST, "NO_FACE").andExpect(status().isBadRequest());
        result(ticket, false, null, null).andExpect(status().isBadRequest());
    }

    @Test void failedRecognitionIsRecordedIdempotentlyButCannotIssueJwt() throws Exception {
        Ticket ticket = create("LOGIN", null);
        validate(ticket, null);
        result(ticket, false, null, "FACE_NOT_RECOGNIZED").andExpect(jsonPath("$.accepted").value(true));
        result(ticket, false, null, "FACE_NOT_RECOGNIZED").andExpect(jsonPath("$.accepted").value(true));
        complete(ticket).andExpect(status().isConflict());
        result(ticket, true, FIRST, null).andExpect(status().isConflict());
    }

    @Test void differentResultCannotOverwriteOriginalRecognition() throws Exception {
        Ticket ticket = recognized();
        result(ticket, true, SECOND, null).andExpect(status().isConflict());
        assertThat(decoder.decode(token(complete(ticket))).getSubject()).isEqualTo(FIRST.toString());
    }

    @Test void lockedAdminAtCallbackIsRejectedAndSameCallbackKeepsDecision() throws Exception {
        Ticket ticket = create("LOGIN", null);
        validate(ticket, null);
        jdbc.update("UPDATE users SET status = 'LOCKED' WHERE id = ?", FIRST);
        result(ticket, true, FIRST, null).andExpect(jsonPath("$.accepted").value(false));
        jdbc.update("UPDATE users SET status = 'ACTIVE' WHERE id = ?", FIRST);
        result(ticket, true, FIRST, null).andExpect(jsonPath("$.accepted").value(false));
        complete(ticket).andExpect(status().isConflict());
    }

    @Test void lockAfterCallbackStillBlocksJwtCompletion() throws Exception {
        Ticket ticket = recognized();
        jdbc.update("UPDATE users SET status = 'LOCKED' WHERE id = ?", FIRST);
        complete(ticket).andExpect(status().isForbidden());
    }

    @Test void recognizedNonAdminOrMissingAccountCannotLogin() throws Exception {
        jdbc.update("UPDATE users SET role = 'TENANT' WHERE id = ?", FIRST);
        for (UUID id : new UUID[] {FIRST, UUID.randomUUID()}) {
            Ticket ticket = create("LOGIN", null);
            validate(ticket, null);
            result(ticket, true, id, null).andExpect(jsonPath("$.accepted").value(false));
            complete(ticket).andExpect(status().isConflict());
        }
    }

    @Test void enrollmentCannotChangeOwnerToRecognizedDifferentAdmin() throws Exception {
        String jwt = passwordToken(FIRST);
        Ticket ticket = create("ENROLL", jwt);
        validate(ticket, jwt);
        result(ticket, true, SECOND, null).andExpect(jsonPath("$.accepted").value(false));
        assertThat(jdbc.queryForObject("SELECT admin_id FROM admin_face_sessions WHERE auth_session_id = ?", UUID.class, ticket.id()))
                .isEqualTo(FIRST);
    }

    @Test void enrollmentCannotBeUsedToCompleteLogin() throws Exception {
        String jwt = passwordToken(FIRST);
        Ticket ticket = create("ENROLL", jwt);
        validate(ticket, jwt);
        result(ticket, true, FIRST, null);
        complete(new Ticket(ticket.id(), "a".repeat(43), "LOGIN")).andExpect(status().isForbidden());
    }

    @Test void knowingSessionIdWithoutCompletionSecretCannotGetJwt() throws Exception {
        Ticket ticket = recognized();
        complete(new Ticket(ticket.id(), "a".repeat(43), "LOGIN")).andExpect(status().isForbidden());
        http.perform(post("/api/auth/face/complete").contentType("application/json")
                .content("{\"authSessionId\":\"" + ticket.id() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void twoConcurrentCompletionsReturnSameJwt() throws Exception {
        Ticket ticket = recognized();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> token(complete(ticket)));
            var second = executor.submit(() -> token(complete(ticket)));
            assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo(second.get(15, TimeUnit.SECONDS));
        }
    }

    @Test void duplicateConcurrentCallbacksKeepSingleDecision() throws Exception {
        Ticket ticket = create("LOGIN", null);
        validate(ticket, null);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> result(ticket, true, FIRST, null).andExpect(status().isOk()));
            var second = executor.submit(() -> result(ticket, true, FIRST, null).andExpect(status().isOk()));
            first.get(15, TimeUnit.SECONDS); second.get(15, TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_face_sessions", Integer.class)).isEqualTo(1);
        token(complete(ticket));
    }

    @Test void deletedAdminCannotCompletePreviouslyVerifiedSession() throws Exception {
        Ticket ticket = recognized();
        jdbc.update("DELETE FROM users WHERE id = ?", FIRST);
        complete(ticket).andExpect(status().isForbidden());
    }

    @Test void finalizedSessionCannotStartAnotherFaceChallenge() throws Exception {
        Ticket ticket = recognized();
        validate(ticket, null).andExpect(jsonPath("$.accepted").value(false));
    }

    @Test void internalLoginValidationDoesNotExposeCompletionSecret() throws Exception {
        Ticket ticket = create("LOGIN", null);
        String body = validate(ticket, null).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(ticket.secret(), "completionToken", "completionHash", "accessToken");
    }

    @Test void faceJwtCannotValidateExistingPasswordEnrollmentSession() throws Exception {
        String passwordJwt = passwordToken(FIRST);
        Ticket enrollment = create("ENROLL", passwordJwt);
        String faceJwt = token(complete(recognized()));
        validate(enrollment, faceJwt).andExpect(status().isForbidden());
    }
}
