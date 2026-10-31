"""Kiểm tra HTTP thật qua TestClient; không gọi dịch vụ bên ngoài."""
import unittest
from fastapi.testclient import TestClient
from src.face_auth.config import Settings
from src.face_auth.main import create_app
from tests.test_workflow import FakeAnalyzer, FakeCore, FakeNotifier
from tests.database_support import create_test_store
from src.face_auth.db.storage import DatabaseUnavailable
from unittest.mock import patch
from unittest.mock import Mock
import threading
import time
from src.face_auth.db.notification_store import NotificationQueue
from src.face_auth.ai.errors import FaceUnavailable


class ApiTests(unittest.TestCase):
    def setUp(self):
        store = create_test_store(self)
        self.core = FakeCore()
        # FakeCore mặc định dùng timestamp cố định cho kiểm thử domain.
        import time
        self.core.validate_session = lambda *args: {
            "accepted": self.core.accept_session, "adminId": 7, "expiresAt": time.time() + 90,
        }
        settings = Settings("http://core", "test", store.database_url)
        self.client = TestClient(create_app(settings, self.core, FakeAnalyzer(), FakeNotifier(), store=store))
        self.client.__enter__()
        self.addCleanup(self.client.__exit__, None, None, None)

    def challenge(self, purpose="LOGIN", headers=None):
        return self.client.post("/api/face/challenges", json={
            "authSessionId": "a" * 32, "purpose": purpose,
        }, headers=headers)

    def test_health_and_no_password_endpoint(self):
        self.assertEqual(self.client.get("/health").status_code, 200)
        self.assertEqual(self.client.post("/api/auth/login").status_code, 404)

    def test_enroll_requires_jwt(self):
        self.assertEqual(self.challenge("ENROLL").status_code, 401)

    def test_core_permission_is_enforced(self):
        self.core.accept_session = False
        self.assertEqual(self.challenge().status_code, 403)

    def test_core_admin_id_is_not_supplied_by_browser(self):
        response = self.challenge("ENROLL", {"Authorization": "Bearer jwt"})
        self.assertEqual(response.status_code, 200)
        self.assertNotIn("adminId", response.json())

    def test_duplicate_session_is_rejected(self):
        self.assertEqual(self.challenge().status_code, 200)
        self.assertEqual(self.challenge().status_code, 409)

    def test_invalid_purpose_and_short_session_are_rejected(self):
        for body in ({"authSessionId": "short", "purpose": "LOGIN"},
                     {"authSessionId": "a" * 32, "purpose": "PASSWORD"}):
            self.assertEqual(self.client.post("/api/face/challenges", json=body).status_code, 422)

    def test_request_size_is_limited(self):
        response = self.client.post("/api/face/challenges/unknown/frames", content=b"x" * (1024 * 1024 + 1))
        self.assertEqual(response.status_code, 413)

    def test_unknown_challenge(self):
        response = self.client.post("/api/face/challenges/unknown/frames", content=b"frame")
        self.assertEqual(response.status_code, 404)

    def test_frame_returns_next_action_and_no_jwt(self):
        challenge_id = self.challenge().json()["challengeId"]
        response = self.client.post(f"/api/face/challenges/{challenge_id}/frames", content=b"frame")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["status"], "IN_PROGRESS")
        self.assertNotIn("jwt", response.json())

    def test_demo_is_disabled_by_default(self):
        self.assertEqual(self.client.get("/demo").status_code, 404)
        self.assertEqual(self.client.get("/demo/assets/face_camera.mjs").status_code, 404)

    def test_demo_serves_local_assets_when_explicitly_enabled(self):
        store = self.client.app.state.workflow.store
        settings = Settings("http://core", "test", store.database_url, enable_demo=True)
        with TestClient(create_app(settings, FakeCore(), FakeAnalyzer(), FakeNotifier(), store=store)) as client:
            page = client.get("/demo")
            self.assertEqual(page.status_code, 200)
            self.assertEqual(page.headers["cache-control"], "no-store")
            self.assertIn("frame-ancestors 'none'", page.headers["content-security-policy"])
            self.assertEqual(client.get("/demo/assets/demo.mjs").status_code, 200)
            self.assertEqual(client.get("/demo/assets/face_camera.mjs").status_code, 200)
            self.assertEqual(client.get("/demo/assets/style.css").status_code, 200)

    def test_status_returns_current_action_without_private_data(self):
        challenge = self.challenge().json()
        response = self.client.get(f"/api/face/challenges/{challenge['challengeId']}")
        self.assertEqual(response.json()["action"], challenge["actions"][0])
        self.assertEqual(response.headers["cache-control"], "no-store")
        for field in ("adminId", "authSessionId", "embedding", "photo", "jwt"):
            self.assertNotIn(field, response.json())

    def test_completed_status_can_be_read_without_resending_frames(self):
        challenge = self.challenge().json()
        workflow = self.client.app.state.workflow
        with workflow.store.edit_challenge(challenge["challengeId"]) as (_, state):
            state.clear()
            state.update({"status": "DONE", "result": "FAILED", "reason": "FACE_NOT_RECOGNIZED"})
        response = self.client.get(f"/api/face/challenges/{challenge['challengeId']}")
        self.assertEqual(response.json()["status"], "FAILED")
        self.assertEqual(response.json()["reason"], "FACE_NOT_RECOGNIZED")
        self.assertEqual(self.core.results, [])

    def test_unknown_status_returns_404(self):
        self.assertEqual(self.client.get("/api/face/challenges/unknown").status_code, 404)

    def test_status_database_error_is_sanitized(self):
        store = self.client.app.state.workflow.store
        with patch.object(store, "read_challenge", side_effect=DatabaseUnavailable("password=secret")):
            response = self.client.get("/api/face/challenges/unknown")
        self.assertEqual(response.status_code, 503)
        self.assertEqual(response.json()["detail"], "DATABASE_UNAVAILABLE")

    def test_browser_cannot_supply_admin_id(self):
        response = self.client.post("/api/face/challenges", json={
            "authSessionId": "a" * 32, "purpose": "ENROLL", "adminId": 99,
        }, headers={"Authorization": "Bearer jwt"})
        self.assertEqual(response.status_code, 422)

    def test_configured_worker_delivers_queue_and_stops_before_client_is_closed(self):
        store = self.client.app.state.workflow.store
        with store.connect() as db:
            NotificationQueue.enqueue(db, "FACE_ENROLLED", "worker-lifecycle", b"test-photo", 7, None, time.time())
        delivered = threading.Event()
        notifier = Mock()

        def send(*args, **kwargs):
            notifier.close.assert_not_called()
            delivered.set()
            return True

        notifier.send.side_effect = send
        settings = Settings("http://core", "test", store.database_url,
                            telegram_token="fake", telegram_chat_id="123")
        with TestClient(create_app(settings, FakeCore(), FakeAnalyzer(), notifier, store=store)) as client:
            self.assertEqual(client.get("/health").status_code, 200)
            self.assertTrue(delivered.wait(3))
        notifier.close.assert_called_once()
        with store.connect() as db:
            status, photo = db.execute("SELECT status, photo FROM telegram_outbox").fetchone()
        self.assertEqual(status, "SENT")
        self.assertIsNone(photo)

    def test_ai_outage_returns_503_without_consuming_challenge_or_notifying(self):
        challenge = self.challenge().json()
        workflow = self.client.app.state.workflow
        endpoint = f"/api/face/challenges/{challenge['challengeId']}/frames"
        with patch.object(workflow.analyzer, "analyze", side_effect=FaceUnavailable("private-detail")):
            response = self.client.post(endpoint, content=b"frame")
        self.assertEqual(response.status_code, 503)
        self.assertEqual(response.json()["detail"], "AI_UNAVAILABLE")
        self.assertEqual(response.headers["cache-control"], "no-store")
        self.assertEqual(self.core.results, [])
        self.assertEqual(workflow.store.read_challenge(challenge["challengeId"])["frames"], 0)
        with workflow.store.connect() as db:
            self.assertEqual(db.execute("SELECT count(*) FROM telegram_outbox").fetchone()[0], 0)
        self.assertEqual(self.client.post(endpoint, content=b"frame").json()["status"], "IN_PROGRESS")

    def test_validation_errors_are_not_cached(self):
        response = self.client.post("/api/face/challenges", json={})
        self.assertEqual(response.status_code, 422)
        self.assertEqual(response.headers["cache-control"], "no-store")


if __name__ == "__main__":
    unittest.main()
