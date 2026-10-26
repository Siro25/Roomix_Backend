import io
import json
import unittest
from unittest.mock import Mock
import httpx
from PIL import Image
from src.face_auth.db.cleanup import cleanup
from src.face_auth.config import Settings
from src.face_auth.integrations.core_client import CoreClient, CoreUnavailable
from src.face_auth.ai.errors import FaceRejected
from tests.database_support import create_test_store
from src.face_auth.integrations.telegram_notifier import TelegramNotifier
from src.face_auth.ai.image_input import decode_image


class AdapterTests(unittest.TestCase):
    def test_core_sends_internal_token_and_enrollment_jwt(self):
        def handle(request):
            self.assertEqual(request.headers["X-Service-Token"], "test-secret")
            self.assertEqual(request.headers["Authorization"], "Bearer admin-jwt")
            self.assertEqual(json.loads(request.content)["purpose"], "ENROLL")
            return httpx.Response(200, json={"accepted": True, "adminId": 7})
        core = CoreClient(Settings("http://core", "test-secret"))
        core.close()
        core.client = httpx.Client(base_url="http://core", headers={"X-Service-Token": "test-secret"},
                                   transport=httpx.MockTransport(handle))
        self.addCleanup(core.close)
        self.assertTrue(core.validate_session("session", "ENROLL", "Bearer admin-jwt")["accepted"])

    def test_core_invalid_response_and_outage_fail_closed(self):
        for response in (httpx.Response(200, json={"accepted": "true"}), httpx.Response(500)):
            core = CoreClient(Settings("http://core", "test-secret"))
            core.close()
            core.client = httpx.Client(base_url="http://core", transport=httpx.MockTransport(lambda request: response))
            with core.client, self.assertRaises(CoreUnavailable):
                core.submit_result({})

    def test_telegram_sends_only_two_allowed_events(self):
        notifier = TelegramNotifier(Settings("http://core", "secret", telegram_token="fake", telegram_chat_id="123"))
        notifier.close()
        notifier.client = Mock()
        notifier.client.post.return_value.json.return_value = {"ok": True}
        self.assertFalse(notifier.send("FACE_LOGIN_SUCCEEDED", "session", b"photo"))
        self.assertFalse(notifier.send("PASSWORD_FAILED", "session", b"photo"))
        notifier.client.post.assert_not_called()
        self.assertTrue(notifier.send("FACE_ENROLLED", "session", b"photo", 7))
        self.assertEqual(notifier.client.post.call_args.kwargs["files"]["photo"][1], b"photo")
        self.assertNotIn("Session:", notifier.client.post.call_args.kwargs["data"]["caption"])

    def test_telegram_failure_does_not_raise(self):
        notifier = TelegramNotifier(Settings("http://core", "secret", telegram_token="fake", telegram_chat_id="123"))
        notifier.close()
        notifier.client = Mock()
        notifier.client.post.side_effect = httpx.ConnectError("offline")
        self.assertFalse(notifier.send("FACE_LOGIN_FAILED", "session", b"photo"))

    def test_invalid_image_is_rejected_before_ai(self):
        with self.assertRaises(FaceRejected):
            decode_image(b"not an image")

    def test_camera_photo_is_reencoded_as_jpeg(self):
        output = io.BytesIO()
        Image.new("RGB", (100, 100), "white").save(output, format="PNG")
        image, photo = decode_image(output.getvalue())
        self.assertEqual(image.size, (100, 100))
        self.assertTrue(photo.startswith(b"\xff\xd8"))

    def test_animated_image_is_rejected(self):
        output = io.BytesIO()
        Image.new("RGB", (100, 100), "red").save(output, format="PNG", save_all=True,
            append_images=[Image.new("RGB", (100, 100), "blue")], duration=100, loop=0)
        with self.assertRaisesRegex(FaceRejected, "UNSUPPORTED_IMAGE"):
            decode_image(output.getvalue())

    def test_exif_orientation_is_applied_before_face_detection(self):
        output = io.BytesIO()
        image = Image.new("RGB", (120, 80), "white")
        exif = Image.Exif()
        exif[274] = 6
        image.save(output, format="JPEG", exif=exif)
        corrected, _ = decode_image(output.getvalue())
        self.assertEqual(corrected.size, (80, 120))

    def test_cleanup_removes_temporary_biometrics_but_keeps_profile(self):
        store = create_test_store(self)
        store.create_challenge("challenge", {"authSessionId": "session", "expiresAt": 10,
                               "status": "ACTIVE", "vectors": [[1]], "photo": "temporary"})
        with store.connect() as db:
            store.save_profile(db, 7, [1], 0)
        self.assertEqual(cleanup(store, 11), 1)
        self.assertEqual(cleanup(store, 12), 0)
        with store.edit_challenge("challenge") as (db, state):
            self.assertEqual(state, {"status": "EXPIRED", "expiresAt": 10})
            self.assertEqual(store.profiles(db), {7: [1]})


if __name__ == "__main__":
    unittest.main()
