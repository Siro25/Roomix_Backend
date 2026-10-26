package com.team4.core.face;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.team4.core.auth.*;
import com.team4.core.auth.AuthDtos.AuthResponse;
import com.team4.core.face.FaceDtos.*;
import com.team4.core.face.FaceSessionRepository.Session;
import com.team4.core.notifications.AuthTelegramAlerts;

@Service
public class FaceSessionService {
    private final FaceSessionRepository sessions;
    private final AdminAccountRepository accounts;
    private final JwtService tokens;
    private final AuthTelegramAlerts alerts;
    public FaceSessionService(FaceSessionRepository sessions, AdminAccountRepository accounts, JwtService tokens, AuthTelegramAlerts alerts) {
        this.sessions = sessions; this.accounts = accounts; this.tokens = tokens; this.alerts = alerts;
    }
    private AdminAccount activeAdmin(UUID id) {
        if (id == null) throw new FaceAuthFailure(403, "ADMIN_REQUIRED");
        return accounts.findById(id).filter(AdminAccount::isActiveAdmin)
                .orElseThrow(() -> new FaceAuthFailure(403, "ADMIN_REQUIRED"));
    }
    private AdminAccount passwordAdmin(Jwt jwt) {
        if (jwt == null) throw new FaceAuthFailure(401, "ADMIN_LOGIN_REQUIRED");
        if (!"PASSWORD".equals(jwt.getClaimAsString("auth_method")) || jwt.getExpiresAt() == null
                || !Instant.now().isBefore(jwt.getExpiresAt())) throw new FaceAuthFailure(403, "PASSWORD_LOGIN_REQUIRED");
        UUID id;
        try { id = UUID.fromString(jwt.getSubject()); }
        catch (IllegalArgumentException exception) { throw new FaceAuthFailure(403, "ADMIN_REQUIRED"); }
        if (!id.toString().equals(jwt.getSubject())) throw new FaceAuthFailure(403, "ADMIN_REQUIRED");
        return activeAdmin(id);
    }
    private Session current(String id, Purpose purpose) {
        Session session = sessions.lock(id);
        if (session.purpose() != purpose) throw new FaceAuthFailure(403, "SESSION_PURPOSE_MISMATCH");
        if (!Instant.now().isBefore(session.expiresAt())) throw new FaceAuthFailure(410, "SESSION_EXPIRED");
        return session;
    }
    @Transactional
    public SessionResponse create(Purpose purpose, Jwt jwt) {
        Instant expiry = Instant.now().plusSeconds(90).truncatedTo(ChronoUnit.MILLIS);
        AdminAccount owner = purpose == Purpose.ENROLL ? passwordAdmin(jwt) : null;
        if (owner != null && jwt.getExpiresAt().isBefore(expiry)) expiry = jwt.getExpiresAt();
        String id = FaceSecrets.randomToken();
        String completion = purpose == Purpose.LOGIN ? FaceSecrets.randomToken() : null;
        sessions.create(id, purpose, owner == null ? null : owner.id(), completion == null ? null : FaceSecrets.hash(completion),
                owner == null ? null : FaceSecrets.hash(jwt.getTokenValue()), expiry);
        return new SessionResponse(id, purpose, expiry.toEpochMilli() / 1000.0, completion);
    }
    @Transactional
    public ValidateResponse validate(ValidateRequest request, Jwt jwt) {
        Session session = current(request.authSessionId(), request.purpose());
        if (!"WAITING".equals(session.state())) return new ValidateResponse(false, null, null);
        if (session.purpose() == Purpose.ENROLL) {
            AdminAccount owner = passwordAdmin(jwt);
            if (!owner.id().equals(session.adminId()) || !FaceSecrets.matches(session.jwtHash(), jwt.getTokenValue())) {
                return new ValidateResponse(false, null, null);
            }
        }
        sessions.validated(session.id());
        return new ValidateResponse(true, session.adminId(), session.expiresAt().toEpochMilli() / 1000.0);
    }
    @Transactional
    public Decision submit(ResultRequest request) {
        if (request.verified() ? request.adminId() == null || request.reason() != null
                : request.adminId() != null || request.reason() == null) throw new FaceAuthFailure(400, "INVALID_FACE_RESULT");
        Session session = current(request.authSessionId(), request.purpose());
        String fingerprint = FaceSecrets.hash(request.purpose() + "|" + request.verified() + "|" + request.adminId() + "|" + request.reason());
        if (session.resultHash() != null) {
            if (!session.resultHash().equals(fingerprint)) throw new FaceAuthFailure(409, "RESULT_CONFLICT");
            return new Decision(Boolean.TRUE.equals(session.decision()));
        }
        if (session.validatedAt() == null) throw new FaceAuthFailure(409, "SESSION_NOT_VALIDATED");
        boolean accepted = true;
        if (request.verified()) {
            accepted = accounts.findById(request.adminId()).filter(AdminAccount::isActiveAdmin).isPresent();
            if (session.purpose() == Purpose.ENROLL) accepted &= request.adminId().equals(session.adminId());
        }
        UUID adminId = session.purpose() == Purpose.ENROLL ? session.adminId() : request.adminId();
        sessions.result(session.id(), adminId, request.verified(), accepted, fingerprint);
        return new Decision(accepted);
    }
    @Transactional
    public AuthResponse complete(CompleteRequest request) {
        Session session = current(request.authSessionId(), Purpose.LOGIN);
        if (!FaceSecrets.matches(session.completionHash(), request.completionToken())) throw new FaceAuthFailure(403, "SESSION_OWNER_REQUIRED");
        if (!"VERIFIED".equals(session.state()) || !Boolean.TRUE.equals(session.decision())) throw new FaceAuthFailure(409, "FACE_NOT_VERIFIED");
        AdminAccount account = activeAdmin(session.adminId());
        Instant issued = session.issuedAt();
        Instant expiry = session.tokenExpiresAt();
        if (issued == null) {
            issued = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            expiry = issued.plusSeconds(tokens.ttlSeconds());
            sessions.issued(session.id(), issued, expiry);
            alerts.faceSucceeded(session.id(), account);
        }
        if (!Instant.now().isBefore(expiry)) throw new FaceAuthFailure(410, "TOKEN_EXPIRED");
        return tokens.issueFaceToken(account, issued, expiry, session.id());
    }
}
