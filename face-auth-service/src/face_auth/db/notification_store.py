"""Các truy vấn hàng đợi Telegram, dùng cùng database của Face."""
from src.face_auth.admin_identity import validate_admin_id

ALLOWED_EVENTS = ("FACE_LOGIN_FAILED", "FACE_ENROLLED")
PHOTO_TTL_SECONDS = 24 * 60 * 60


class NotificationQueue:
    def __init__(self, store):
        self.store = store

    @staticmethod
    def enqueue(db, event, session_id, photo, admin_id, reason, now):
        if event not in ALLOWED_EVENTS or not photo:
            raise ValueError("INVALID_TELEGRAM_EVENT")
        db.execute("""INSERT INTO telegram_outbox
            (auth_session_id, event_type, admin_id, reason, photo, created_at, available_at, expires_at)
            VALUES (%s, %s, %s, %s, %s, to_timestamp(%s), to_timestamp(%s), to_timestamp(%s))
            ON CONFLICT (auth_session_id, event_type) DO NOTHING""",
            (session_id, event, str(validate_admin_id(admin_id)) if admin_id is not None else None,
             reason, photo, now, now, now + PHOTO_TTL_SECONDS))

    @staticmethod
    def discard_expired(db, now):
        # Không chờ worker khác đang gửi. Chỉ dọn những dòng lấy được khóa.
        result = db.execute("""UPDATE telegram_outbox
            SET status = 'FAILED', photo = NULL, completed_at = to_timestamp(%s)
            WHERE id IN (SELECT id FROM telegram_outbox
                WHERE status = 'PENDING' AND expires_at <= to_timestamp(%s)
                FOR UPDATE SKIP LOCKED)""", (now, now))
        return result.rowcount

    @staticmethod
    def next_message(db, now):
        return db.execute("""SELECT id, event_type, photo, admin_id, reason, created_at, attempts
            FROM telegram_outbox
            WHERE status = 'PENDING' AND available_at <= to_timestamp(%s)
                AND expires_at > to_timestamp(%s)
            ORDER BY available_at, id LIMIT 1 FOR UPDATE SKIP LOCKED""", (now, now)).fetchone()

    @staticmethod
    def mark_result(db, message_id, sent, attempts, now):
        attempts += 1
        if sent or attempts >= 5:
            db.execute("""UPDATE telegram_outbox SET status = %s, attempts = %s,
                photo = NULL, completed_at = to_timestamp(%s) WHERE id = %s""",
                ("SENT" if sent else "FAILED", attempts, now, message_id))
        else:
            retry_seconds = min(5 * 2 ** (attempts - 1), 300)
            db.execute("""UPDATE telegram_outbox SET attempts = %s,
                available_at = to_timestamp(%s) WHERE id = %s""",
                (attempts, now + retry_seconds, message_id))
