"""Kiểm thử PostgreSQL thật: transaction, khóa hàng, JSONB và ràng buộc."""
import threading
import unittest
from pathlib import Path
from unittest.mock import patch
from src.face_auth.config import Settings
from src.face_auth.db.storage import Store
from tests.database_support import create_test_store


class ConfigTests(unittest.TestCase):
    def test_postgresql_is_required(self):
        with patch.dict("os.environ", {"CORE_URL": "http://core", "FACE_SERVICE_TOKEN": "secret",
                                      "FACE_DATABASE_URL": "sqlite:///face.db"}, clear=True):
            with self.assertRaises(ValueError):
                Settings.from_env()
        with self.assertRaises(ValueError):
            Store("face.db")


class StorageTests(unittest.TestCase):
    def test_bigint_migration_preserves_profiles_pending_photos_and_is_repeatable(self):
        # Tái tạo schema cũ chỉ trong schema ngẫu nhiên của test này.
        old_schema = Path("src/face_auth/db/schema.sql").read_text(encoding="utf-8")
        old_schema = old_schema.replace(
            "admin_id TEXT PRIMARY KEY CHECK (admin_id ~ '^([1-9][0-9]{0,18}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})$')",
            "admin_id BIGINT PRIMARY KEY CHECK (admin_id > 0)",
        ).replace("admin_id TEXT,", "admin_id BIGINT,")
        with self.store.connect() as db:
            db.execute("DROP TABLE face_profiles, telegram_outbox")
            db.execute(old_schema)
            db.execute("INSERT INTO face_profiles (admin_id, embedding) VALUES (7, '[1.0, 0.0]')")
            db.execute("""INSERT INTO telegram_outbox
                (auth_session_id, event_type, admin_id, photo, created_at, available_at, expires_at)
                VALUES ('old-session', 'FACE_ENROLLED', 7, %s, now(), now(), now() + interval '1 day')""",
                (b"old-photo",))
        for _ in range(2):
            migrated = Store(self.store.database_url, schema=self.store.schema)
            with migrated.connect() as db:
                self.assertEqual(migrated.profiles(db), {7: [1.0, 0.0]})
                self.assertEqual(db.execute("SELECT admin_id, photo FROM telegram_outbox").fetchone(),
                                 ("7", b"old-photo"))
                self.assertEqual(db.execute("SELECT pg_typeof(admin_id)::text FROM face_profiles").fetchone()[0],
                                 "text")
                migrated.save_profile(db, "10000000-0000-4000-8000-000000000005", [0.0, 1.0], 1000)
            # Lượt sau chỉ còn dữ liệu cũ để đối chiếu bảo toàn migration.
            with migrated.connect() as db:
                db.execute("DELETE FROM face_profiles WHERE admin_id <> '7'")
        self.assertEqual(self.store.read_challenge("challenge")["status"], "ACTIVE")

    def setUp(self):
        self.store = create_test_store(self)
        self.store.create_challenge("challenge", {
            "authSessionId": "session", "status": "ACTIVE", "expiresAt": 2000,
        })

    def test_duplicate_core_session_is_rejected(self):
        with self.assertRaises(ValueError):
            self.store.create_challenge("another", {"authSessionId": "session"})

    def test_failed_transaction_does_not_change_profile_or_challenge(self):
        with self.assertRaises(RuntimeError):
            with self.store.edit_challenge("challenge") as (db, state):
                state["status"] = "DONE"
                self.store.save_profile(db, 7, [1.0, 0.0], 1000)
                raise RuntimeError("rollback")
        with self.store.edit_challenge("challenge") as (db, state):
            self.assertEqual(state["status"], "ACTIVE")
            self.assertEqual(self.store.profiles(db), {})

    def test_reenrollment_replaces_one_profile(self):
        with self.store.connect() as db:
            self.store.save_profile(db, 7, [1.0, 0.0], 1000)
            self.store.save_profile(db, 7, [0.0, 1.0], 1001)
        with self.store.connect() as db:
            self.assertEqual(self.store.profiles(db), {7: [0.0, 1.0]})
            timestamp = db.execute("SELECT updated_at FROM face_profiles WHERE admin_id = '7'").fetchone()[0]
            self.assertEqual(timestamp.timestamp(), 1001)

    def test_two_requests_cannot_overwrite_the_same_step(self):
        started = threading.Event()
        finished = threading.Event()
        errors = []

        def second_request():
            started.set()
            try:
                with self.store.edit_challenge("challenge") as (_, state):
                    state["frames"] += 1
            except Exception as exc:
                errors.append(exc)
            finally:
                finished.set()

        with self.store.edit_challenge("challenge") as (_, state):
            state["frames"] = 1
            worker = threading.Thread(target=second_request)
            worker.start()
            self.assertTrue(started.wait(2))
            self.assertFalse(finished.wait(0.2))
        worker.join(6)
        self.assertFalse(worker.is_alive())
        self.assertEqual(errors, [])
        with self.store.edit_challenge("challenge") as (_, state):
            self.assertEqual(state["frames"], 2)


if __name__ == "__main__":
    unittest.main()
