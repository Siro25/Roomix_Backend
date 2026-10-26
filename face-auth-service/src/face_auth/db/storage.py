"""PostgreSQL riêng của Face: hồ sơ Admin và trạng thái thử thách."""
from contextlib import contextmanager
from pathlib import Path
import psycopg
from psycopg import sql
from psycopg.types.json import Jsonb
from src.face_auth.db.notification_store import NotificationQueue
from src.face_auth.admin_identity import stored_admin_id, validate_admin_id


class DatabaseUnavailable(Exception):
    """Không đưa bí mật kết nối database ra phản hồi API."""


class Store:
    def __init__(self, database_url, schema="public"):
        if not database_url.startswith(("postgresql://", "postgres://")):
            raise ValueError("FACE_DATABASE_URL phải là URL PostgreSQL")
        self.database_url = database_url
        self.schema = schema
        with self.connect() as db:
            if schema != "public":
                db.execute(sql.SQL("CREATE SCHEMA IF NOT EXISTS {}").format(sql.Identifier(schema)))
            # Hai tiến trình không được nâng schema đồng thời.
            db.execute("SELECT pg_advisory_xact_lock(720032)")
            migration = Path(__file__).parent / "migrations" / "001_admin_id_text.sql"
            db.execute(migration.read_text(encoding="utf-8"))
            script = Path(__file__).with_name("schema.sql").read_text(encoding="utf-8")
            db.execute(script)

    @contextmanager
    def connect(self):
        try:
            with psycopg.connect(self.database_url, connect_timeout=5) as db:
                db.execute(sql.SQL("SET LOCAL search_path TO {}").format(sql.Identifier(self.schema)))
                db.execute("SET LOCAL lock_timeout = '5s'")
                yield db
        except (psycopg.OperationalError, psycopg.errors.LockNotAvailable) as exc:
            raise DatabaseUnavailable("DATABASE_UNAVAILABLE") from exc

    def create_challenge(self, challenge_id, state):
        try:
            with self.connect() as db:
                db.execute("""INSERT INTO face_challenges (id, auth_session_id, state)
                    VALUES (%s, %s, %s)""", (challenge_id, state["authSessionId"], Jsonb(state)))
        except psycopg.errors.UniqueViolation as exc:
            raise ValueError("SESSION_ALREADY_STARTED") from exc

    @contextmanager
    def edit_challenge(self, challenge_id):
        with self.connect() as db:
            # Khóa riêng một phiên, tránh hai request dùng cùng một bước.
            row = db.execute("SELECT state FROM face_challenges WHERE id = %s FOR UPDATE",
                             (challenge_id,)).fetchone()
            if row is None:
                raise KeyError(challenge_id)
            state = row[0]
            if state.get("purpose") == "ENROLL":
                # Hai Admin không được đăng ký trùng khuôn mặt đồng thời.
                db.execute("SELECT pg_advisory_xact_lock(720031)")
            yield db, state
            db.execute("UPDATE face_challenges SET state = %s WHERE id = %s",
                       (Jsonb(state), challenge_id))

    def read_challenge(self, challenge_id):
        with self.connect() as db:
            row = db.execute("SELECT state FROM face_challenges WHERE id = %s", (challenge_id,)).fetchone()
            if row is None:
                raise KeyError(challenge_id)
            return row[0]

    @staticmethod
    def profiles(db):
        return {stored_admin_id(admin_id): vector
                for admin_id, vector in db.execute("SELECT admin_id, embedding FROM face_profiles")}

    @staticmethod
    def save_profile(db, admin_id, vector, now):
        db.execute("""INSERT INTO face_profiles (admin_id, embedding, updated_at)
            VALUES (%s, %s, to_timestamp(%s))
            ON CONFLICT(admin_id) DO UPDATE SET
            embedding=excluded.embedding, updated_at=excluded.updated_at""",
            (str(validate_admin_id(admin_id)), Jsonb(vector), now))

    def cleanup_expired(self, now):
        with self.connect() as db:
            result = db.execute("""UPDATE face_challenges
                SET state = jsonb_build_object('status', 'EXPIRED', 'expiresAt', state->'expiresAt')
                WHERE state->>'status' NOT IN ('DONE', 'EXPIRED')
                  AND (state->>'expiresAt')::double precision <= %s""", (now,))
            return result.rowcount + NotificationQueue.discard_expired(db, now)
