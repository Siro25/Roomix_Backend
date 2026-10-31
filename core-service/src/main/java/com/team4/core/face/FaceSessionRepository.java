package com.team4.core.face;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.team4.core.face.FaceDtos.Purpose;

@Repository
public class FaceSessionRepository {
    public record Session(String id, Purpose purpose, UUID adminId, String completionHash, String jwtHash,
            String state, Instant validatedAt, Instant expiresAt, String resultHash, Boolean decision,
            Instant issuedAt, Instant tokenExpiresAt) {
        @Override public String toString() { return "FaceSession[REDACTED]"; }
    }
    private final JdbcTemplate jdbc;
    public FaceSessionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private static Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
    public void create(String id, Purpose purpose, UUID adminId, String completionHash, String jwtHash, Instant expiry) {
        jdbc.update("""
                INSERT INTO admin_face_sessions
                (auth_session_id, purpose, admin_id, completion_hash, enrollment_jwt_hash, expires_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, purpose.name(), adminId, completionHash, jwtHash, Timestamp.from(expiry));
    }
    public Session lock(String id) {
        return jdbc.query("SELECT * FROM admin_face_sessions WHERE auth_session_id = ? FOR UPDATE", (row, index) ->
                new Session(row.getString("auth_session_id"), Purpose.valueOf(row.getString("purpose")),
                        row.getObject("admin_id", UUID.class), row.getString("completion_hash"), row.getString("enrollment_jwt_hash"),
                        row.getString("state"), instant(row.getTimestamp("validated_at")), instant(row.getTimestamp("expires_at")),
                        row.getString("result_hash"), row.getObject("decision_accepted", Boolean.class),
                        instant(row.getTimestamp("jwt_issued_at")), instant(row.getTimestamp("jwt_expires_at"))), id)
                .stream().findFirst().orElseThrow(() -> new FaceAuthFailure(404, "SESSION_NOT_FOUND"));
    }
    public void validated(String id) {
        jdbc.update("UPDATE admin_face_sessions SET validated_at = COALESCE(validated_at, CURRENT_TIMESTAMP) WHERE auth_session_id = ?", id);
    }
    public void result(String id, UUID adminId, boolean verified, boolean accepted, String fingerprint) {
        jdbc.update("""
                UPDATE admin_face_sessions SET admin_id = ?, state = ?, result_hash = ?, decision_accepted = ?
                WHERE auth_session_id = ?
                """, adminId, verified && accepted ? "VERIFIED" : "FAILED", fingerprint, accepted, id);
    }
    public void issued(String id, Instant now, Instant expiresAt) {
        jdbc.update("UPDATE admin_face_sessions SET jwt_issued_at = ?, jwt_expires_at = ? WHERE auth_session_id = ?",
                Timestamp.from(now), Timestamp.from(expiresAt), id);
    }
}
