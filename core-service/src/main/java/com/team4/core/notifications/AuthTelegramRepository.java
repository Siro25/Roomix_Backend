package com.team4.core.notifications;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthTelegramRepository {
    public record Message(UUID id, String text, int attempts, Instant expiresAt) {
        @Override public String toString() { return "TelegramMessage[REDACTED]"; }
    }
    private final JdbcTemplate jdbc;
    public AuthTelegramRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void enqueue(UUID id, String key, AuthAlertType event, String text, Instant now) {
        jdbc.update("""
                INSERT INTO auth_telegram_outbox
                (id, event_key, event_type, message, created_at, available_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (event_key) DO NOTHING
                """, id, key, event.name(), text, Timestamp.from(now), Timestamp.from(now), Timestamp.from(now.plusSeconds(86400)));
    }

    public void discardExpired(Instant now) {
        jdbc.update("""
                UPDATE auth_telegram_outbox SET status = 'FAILED', message = NULL, completed_at = ?
                WHERE id IN (SELECT id FROM auth_telegram_outbox
                    WHERE status = 'PENDING' AND expires_at <= ? FOR UPDATE SKIP LOCKED)
                """, Timestamp.from(now), Timestamp.from(now));
    }

    public Optional<Message> lockNext(Instant now) {
        return jdbc.query("""
                SELECT id, message, attempts, expires_at FROM auth_telegram_outbox
                WHERE status = 'PENDING' AND available_at <= ? AND expires_at > ?
                ORDER BY available_at, id LIMIT 1 FOR UPDATE SKIP LOCKED
                """, (row, index) -> new Message(row.getObject("id", UUID.class), row.getString("message"),
                        row.getInt("attempts"), row.getTimestamp("expires_at").toInstant()), Timestamp.from(now), Timestamp.from(now))
                .stream().findFirst();
    }

    public void finish(Message message, TelegramTextSender.Result result, Instant now) {
        int attempts = message.attempts() + 1;
        long delay = Math.max(5L << message.attempts(), result.retryAfterSeconds());
        Instant next = now.plusSeconds(delay);
        if (result.sent() || attempts >= 5 || !next.isBefore(message.expiresAt())) {
            jdbc.update("""
                    UPDATE auth_telegram_outbox SET status = ?, attempts = ?, message = NULL, completed_at = ?
                    WHERE id = ?
                    """, result.sent() ? "SENT" : "FAILED", attempts, Timestamp.from(now), message.id());
        } else {
            jdbc.update("UPDATE auth_telegram_outbox SET attempts = ?, available_at = ? WHERE id = ?",
                    attempts, Timestamp.from(next), message.id());
        }
    }
}
