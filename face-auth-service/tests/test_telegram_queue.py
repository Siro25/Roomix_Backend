"""PostgreSQL thật; Telegram giả lập, không gửi tin nhắn ra ngoài."""
import threading
import unittest
from unittest.mock import Mock
from src.face_auth.db.notification_store import NotificationQueue
from src.face_auth.db.storage import Store
from src.face_auth.services.telegram_worker import TelegramWorker
from tests.database_support import create_test_store


class TelegramQueueTests(unittest.TestCase):
    def setUp(self):
        self.store = create_test_store(self)
        self.now = 1000.0
        self.notifier = Mock()
        self.notifier.send.return_value = True
        self.worker = TelegramWorker(self.store, self.notifier, clock=lambda: self.now)

    def enqueue(self, event="FACE_LOGIN_FAILED", session="private-core-session"):
        with self.store.connect() as db:
            NotificationQueue.enqueue(db, event, session, b"current-frame", None, "NO_FACE", self.now)

    def message(self):
        with self.store.connect() as db:
            return db.execute("SELECT status, attempts, photo FROM telegram_outbox ORDER BY id LIMIT 1").fetchone()

    def test_only_two_event_types_can_be_queued(self):
        for event in ("PASSWORD_FAILED", "FACE_LOGIN_SUCCEEDED", "FACE_ENROLL_FAILED"):
            with self.subTest(event=event), self.assertRaises(ValueError):
                self.enqueue(event)
        self.assertIsNone(self.message())

    def test_same_session_and_event_are_not_queued_twice(self):
        self.enqueue()
        self.enqueue()
        with self.store.connect() as db:
            self.assertEqual(db.execute("SELECT count(*) FROM telegram_outbox").fetchone()[0], 1)

    def test_success_removes_photo_and_does_not_send_again(self):
        self.enqueue()
        self.assertTrue(self.worker.deliver_next())
        self.assertEqual(self.message(), ("SENT", 1, None))
        self.assertFalse(self.worker.deliver_next())
        self.assertEqual(self.notifier.send.call_count, 1)

    def test_retry_waits_until_scheduled_time_then_delivers_original_photo(self):
        self.enqueue()
        self.notifier.send.return_value = False
        self.worker.deliver_next()
        self.assertEqual(self.message(), ("PENDING", 1, b"current-frame"))
        self.assertFalse(self.worker.deliver_next())
        self.assertEqual(self.notifier.send.call_count, 1)
        self.now += 5
        self.notifier.send.return_value = True
        self.assertTrue(self.worker.deliver_next())
        self.assertEqual(self.message(), ("SENT", 2, None))
        self.assertEqual(self.notifier.send.call_args.args[2], b"current-frame")

    def test_five_failed_attempts_stop_retrying_and_remove_photo(self):
        self.enqueue()
        self.notifier.send.return_value = False
        for _ in range(5):
            self.assertTrue(self.worker.deliver_next())
            self.now += 300
        self.assertEqual(self.message(), ("FAILED", 5, None))
        self.assertFalse(self.worker.deliver_next())

    def test_restart_can_deliver_pending_event(self):
        self.enqueue("FACE_ENROLLED")
        restarted_store = Store(self.store.database_url, schema=self.store.schema)
        restarted_worker = TelegramWorker(restarted_store, self.notifier, clock=lambda: self.now)
        self.assertTrue(restarted_worker.deliver_next())
        self.assertEqual(self.message(), ("SENT", 1, None))

    def test_expired_photo_is_removed_without_sending(self):
        self.enqueue()
        self.now += 24 * 60 * 60 + 1
        self.assertFalse(self.worker.deliver_next())
        self.assertEqual(self.message(), ("FAILED", 0, None))
        self.notifier.send.assert_not_called()

    def test_cleanup_also_removes_expired_queue_photos(self):
        self.enqueue()
        self.assertEqual(self.store.cleanup_expired(self.now + 24 * 60 * 60 + 1), 1)
        self.assertEqual(self.message(), ("FAILED", 0, None))

    def test_adapter_exception_is_retried_without_logging_token(self):
        self.enqueue()
        self.notifier.send.side_effect = RuntimeError("private-bot-token")
        with self.assertLogs("src.face_auth.services.telegram_worker", level="WARNING") as logs:
            self.worker.deliver_next()
        self.assertNotIn("private-bot-token", " ".join(logs.output))
        self.assertEqual(self.message(), ("PENDING", 1, b"current-frame"))

    def test_telegram_gets_event_id_and_occurrence_time_not_core_session(self):
        self.enqueue()
        self.now += 60
        self.worker.deliver_next()
        call = self.notifier.send.call_args
        self.assertTrue(call.args[1].isdigit())
        self.assertNotIn("private-core-session", str(call))
        self.assertEqual(call.kwargs["occurred_at"].timestamp(), 1000)

    def test_profile_result_and_notification_are_rolled_back_together(self):
        self.store.create_challenge("challenge", {"authSessionId": "session", "status": "ACTIVE"})
        with self.assertRaises(RuntimeError):
            with self.store.edit_challenge("challenge") as (db, state):
                self.store.save_profile(db, 7, [1.0], self.now)
                NotificationQueue.enqueue(db, "FACE_ENROLLED", "session", b"photo", 7, None, self.now)
                state["status"] = "DONE"
                raise RuntimeError("commit failed")
        self.assertIsNone(self.message())
        self.assertEqual(self.store.read_challenge("challenge")["status"], "ACTIVE")
        with self.store.connect() as db:
            self.assertEqual(self.store.profiles(db), {})

    def test_two_workers_do_not_deliver_the_same_row_at_the_same_time(self):
        self.enqueue()
        entered = threading.Event()
        release = threading.Event()
        errors = []

        def blocked_send(*args, **kwargs):
            entered.set()
            if not release.wait(5):
                raise RuntimeError("test did not release sender")
            return True

        self.notifier.send.side_effect = blocked_send

        def first_worker():
            try:
                self.worker.deliver_next()
            except Exception as exc:
                errors.append(exc)

        thread = threading.Thread(target=first_worker)
        thread.start()
        try:
            self.assertTrue(entered.wait(3))
            other = TelegramWorker(self.store, self.notifier, clock=lambda: self.now)
            self.assertFalse(other.deliver_next())
        finally:
            release.set()
            thread.join(7)
        self.assertFalse(thread.is_alive())
        self.assertEqual(errors, [])
        self.assertEqual(self.notifier.send.call_count, 1)
        self.assertEqual(self.message(), ("SENT", 1, None))


if __name__ == "__main__":
    unittest.main()
