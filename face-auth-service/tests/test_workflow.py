import unittest
from dataclasses import replace
from unittest.mock import patch
from types import SimpleNamespace

from src.face_auth.config import Settings
from src.face_auth.integrations.core_client import CoreUnavailable
from src.face_auth.ai.errors import FaceRejected
from src.face_auth.ai.matching import identify, normalize
from tests.database_support import create_test_store
from src.face_auth.services.face_workflow import FaceWorkflow, WorkflowError


FIRST_FACE = normalize([1.0, 0.0, 0.0])
OTHER_FACE = normalize([0.0, 1.0, 0.0])


class FakeCore:
    def __init__(self):
        self.accept_session = True
        self.accept_result = True
        self.available = True
        self.results = []

    def validate_session(self, session_id, purpose, authorization):
        return {"accepted": self.accept_session, "adminId": 7, "expiresAt": 2000.0}

    def submit_result(self, result):
        if not self.available:
            raise CoreUnavailable("CORE_UNAVAILABLE")
        self.results.append(result.copy())
        return {"accepted": self.accept_result}

    def close(self):
        pass


class FakeAnalyzer:
    pose = "CENTER"
    vector = FIRST_FACE
    failure = None

    def analyze(self, content):
        if self.failure:
            raise FaceRejected(self.failure, b"sanitized-photo")
        return SimpleNamespace(embedding=self.vector, pose=self.pose, photo=b"sanitized-photo")


class FakeNotifier:
    def close(self):
        pass

    def send(self, *args):
        pass


class WorkflowTests(unittest.TestCase):
    def test_uuid_enrollment_and_login_return_the_same_core_id(self):
        admin_id = "10000000-0000-4000-8000-000000000005"
        with patch.object(self.core, "validate_session", return_value={
            "accepted": True, "adminId": admin_id, "expiresAt": 2000.0,
        }):
            self.assertEqual(self.complete(self.start("ENROLL"))["status"], "SUCCEEDED")
        self.assertEqual(self.notifications()[0][-1], admin_id)
        self.assertEqual(self.complete(self.start(session_id="b" * 32))["status"], "SUCCEEDED")
        self.assertEqual([result["adminId"] for result in self.core.results], [admin_id, admin_id])

    def test_uuid_reenrollment_does_not_treat_own_face_as_another_admin(self):
        admin_id = "10000000-0000-4000-8000-000000000005"
        self.profile(admin_id)
        with patch.object(self.core, "validate_session", return_value={
            "accepted": True, "adminId": admin_id, "expiresAt": 2000.0,
        }):
            self.assertEqual(self.complete(self.start("ENROLL"))["status"], "SUCCEEDED")
        with self.store.connect() as db:
            self.assertEqual(list(self.store.profiles(db)), [admin_id])

    def test_uuid_admin_cannot_claim_face_of_legacy_numeric_admin(self):
        self.profile(7)
        with patch.object(self.core, "validate_session", return_value={
            "accepted": True, "adminId": "10000000-0000-4000-8000-000000000005", "expiresAt": 2000.0,
        }):
            self.assertEqual(self.complete(self.start("ENROLL"))["reason"], "FACE_ALREADY_ENROLLED")
        self.assertEqual(self.notifications(), [])

    def setUp(self):
        self.store = create_test_store(self)
        self.core = FakeCore()
        self.analyzer = FakeAnalyzer()
        self.now = 1000.0
        self.workflow = FaceWorkflow(
            Settings("http://core", "secret", telegram_token="fake", telegram_chat_id="123"),
            self.store, self.core, self.analyzer, clock=lambda: self.now,
        )

    def notifications(self):
        with self.store.connect() as db:
            return db.execute("SELECT event_type, id, photo, admin_id FROM telegram_outbox ORDER BY id").fetchall()

    def start(self, purpose="LOGIN", session_id="a" * 32):
        return self.workflow.start(session_id, purpose, "Bearer admin-jwt")

    def frame(self, challenge, pose):
        self.now += 0.3
        self.analyzer.pose = pose
        return self.workflow.process_frame(challenge["challengeId"], b"frame")

    def complete(self, challenge):
        for pose in challenge["actions"]:
            self.frame(challenge, pose)
            response = self.frame(challenge, pose)
        return response

    def profile(self, admin_id=7, vector=FIRST_FACE):
        with self.store.connect() as db:
            self.store.save_profile(db, admin_id, vector, self.now)

    def test_enrollment_requires_authenticated_admin(self):
        with self.assertRaises(WorkflowError) as error:
            self.workflow.start("a" * 32, "ENROLL", None)
        self.assertEqual(error.exception.status_code, 401)
        self.core.accept_session = False
        with self.assertRaises(WorkflowError):
            self.start("ENROLL")

    def test_enrollment_uses_core_admin_id_and_sends_photo(self):
        response = self.complete(self.start("ENROLL"))
        self.assertEqual(response["status"], "SUCCEEDED")
        with self.store.connect() as db:
            self.assertEqual(list(self.store.profiles(db)), [7])
        self.assertEqual(self.notifications()[0][0], "FACE_ENROLLED")
        self.assertEqual(self.notifications()[0][-1], "7")

    def test_login_identifies_admin_without_claimed_id_and_does_not_send_photo(self):
        self.profile(42)
        response = self.complete(self.start())
        self.assertEqual(response["status"], "SUCCEEDED")
        self.assertEqual(self.core.results[0]["adminId"], 42)
        self.assertEqual(self.notifications(), [])

    def test_unknown_face_sends_failed_login_photo(self):
        response = self.complete(self.start())
        self.assertEqual(response["reason"], "FACE_NOT_RECOGNIZED")
        self.assertEqual(self.notifications()[0][0], "FACE_LOGIN_FAILED")

    def test_core_can_reject_recognized_admin(self):
        self.profile()
        self.core.accept_result = False
        response = self.complete(self.start())
        self.assertEqual(response["reason"], "CORE_REJECTED")
        self.assertEqual(self.notifications()[0][0], "FACE_LOGIN_FAILED")

    def test_core_rejection_does_not_save_enrollment(self):
        self.core.accept_result = False
        self.assertEqual(self.complete(self.start("ENROLL"))["status"], "FAILED")
        with self.store.connect() as db:
            self.assertEqual(self.store.profiles(db), {})
        self.assertEqual(self.notifications(), [])

    def test_multiple_faces_fail_and_send_sanitized_photo(self):
        challenge = self.start()
        self.analyzer.failure = "MULTIPLE_FACES"
        response = self.frame(challenge, "CENTER")
        self.assertEqual(response["reason"], "MULTIPLE_FACES")
        self.assertEqual(self.notifications()[0][2], b"sanitized-photo")

    def test_failed_enrollment_does_not_send_photo(self):
        self.analyzer.failure = "NO_FACE"
        self.assertEqual(self.frame(self.start("ENROLL"), "CENTER")["status"], "FAILED")
        self.assertEqual(self.notifications(), [])

    def test_cannot_switch_person_during_liveness(self):
        challenge = self.start()
        self.frame(challenge, "CENTER")
        self.analyzer.vector = OTHER_FACE
        self.assertEqual(self.frame(challenge, "CENTER")["reason"], "FACE_CHANGED")

    def test_static_face_never_completes_turn_challenge(self):
        challenge = self.start()
        for _ in range(12):
            response = self.frame(challenge, "CENTER")
        self.assertEqual(response["status"], "IN_PROGRESS")
        self.assertEqual(self.core.results, [])

    def test_expired_session_and_replay_are_rejected(self):
        challenge = self.start()
        self.now += 91
        with self.assertRaises(WorkflowError) as error:
            self.frame(challenge, "CENTER")
        self.assertEqual(error.exception.status_code, 410)
        with self.assertRaises(WorkflowError):
            self.start()

    def test_completed_challenge_cannot_be_reused(self):
        challenge = self.start()
        self.complete(challenge)
        with self.assertRaises(WorkflowError) as error:
            self.frame(challenge, "CENTER")
        self.assertEqual(error.exception.status_code, 409)

    def test_core_outage_keeps_original_result_for_retry(self):
        challenge = self.start()
        self.core.available = False
        with self.assertRaises(CoreUnavailable):
            self.complete(challenge)
        self.assertEqual(self.notifications(), [])
        self.core.available = True
        self.analyzer.vector = OTHER_FACE
        response = self.frame(challenge, "CENTER")
        self.assertEqual(response["reason"], "FACE_NOT_RECOGNIZED")
        self.assertEqual(len(self.notifications()), 1)

    def test_duplicate_face_cannot_be_registered_to_another_admin(self):
        self.profile(42)
        self.assertEqual(self.complete(self.start("ENROLL"))["reason"], "FACE_ALREADY_ENROLLED")

    def test_matching_rejects_two_similar_profiles(self):
        admin_id, reason = identify(FIRST_FACE, {1: FIRST_FACE, 2: FIRST_FACE}, 0.8, 0.05)
        self.assertIsNone(admin_id)
        self.assertEqual(reason, "AMBIGUOUS_FACE")

    def test_liveness_timeout_reports_failure_and_sends_photo(self):
        challenge = self.start()
        self.frame(challenge, "CENTER")
        self.now += 13
        response = self.frame(challenge, "CENTER")
        self.assertEqual(response["reason"], "LIVENESS_TIMEOUT")
        self.assertFalse(self.core.results[0]["verified"])
        self.assertEqual(self.notifications()[0][0], "FACE_LOGIN_FAILED")

    def test_motion_blur_can_recover_without_failing_or_notifying_core(self):
        challenge = self.start("ENROLL")
        self.analyzer.failure = "FACE_TOO_BLURRY"
        response = self.frame(challenge, "CENTER")
        self.assertEqual(response["status"], "IN_PROGRESS")
        self.assertEqual(response["reason"], "FACE_TOO_BLURRY")
        self.assertEqual(self.core.results, [])
        self.assertEqual(self.notifications(), [])
        self.analyzer.failure = None
        self.assertEqual(self.complete(challenge)["status"], "SUCCEEDED")

    def test_motion_blur_resets_consecutive_good_frames(self):
        challenge = self.start("ENROLL")
        self.frame(challenge, "CENTER")
        self.analyzer.failure = "FACE_TOO_BLURRY"
        self.frame(challenge, "CENTER")
        self.analyzer.failure = None
        self.frame(challenge, "CENTER")
        state = self.store.read_challenge(challenge["challengeId"])
        self.assertEqual(state["step"], 0)
        self.assertEqual(state["hits"], 1)

    def test_persistent_blur_still_times_out_and_cannot_enroll(self):
        challenge = self.start("ENROLL")
        self.analyzer.failure = "FACE_TOO_BLURRY"
        self.frame(challenge, "CENTER")
        self.now += 13
        response = self.frame(challenge, "CENTER")
        self.assertEqual(response["status"], "FAILED")
        self.assertEqual(response["reason"], "LIVENESS_TIMEOUT")
        self.assertFalse(self.core.results[0]["verified"])
        with self.store.connect() as db:
            self.assertEqual(self.store.profiles(db), {})

    def test_blur_cannot_bypass_total_frame_limit(self):
        challenge = self.start("ENROLL")
        with self.store.edit_challenge(challenge["challengeId"]) as (_, state):
            state["frames"] = 119
        self.analyzer.failure = "FACE_TOO_BLURRY"
        response = self.frame(challenge, "CENTER")
        self.assertEqual(response["status"], "FAILED")
        self.assertEqual(response["reason"], "LIVENESS_FAILED")

    def test_center_frames_during_turn_are_not_added_to_profile(self):
        challenge = self.start("ENROLL")
        self.frame(challenge, "CENTER")
        self.frame(challenge, "CENTER")
        for _ in range(3):
            self.frame(challenge, "CENTER")
        with self.store.edit_challenge(challenge["challengeId"]) as (_, state):
            self.assertEqual(len(state["vectors"]), 2)
            self.assertEqual(state["step"], 1)

    def test_no_photo_is_queued_when_telegram_is_not_configured(self):
        self.workflow.settings = replace(self.workflow.settings, telegram_token="", telegram_chat_id="")
        self.assertEqual(self.complete(self.start())["status"], "FAILED")
        self.assertEqual(self.notifications(), [])

    def test_reading_final_result_does_not_queue_another_notification(self):
        challenge = self.start()
        self.complete(challenge)
        self.assertEqual(self.workflow.status(challenge["challengeId"])["status"], "FAILED")
        self.assertEqual(len(self.notifications()), 1)

    def test_core_retry_does_not_keep_photo_when_telegram_is_disabled(self):
        self.workflow.settings = replace(self.workflow.settings, telegram_token="", telegram_chat_id="")
        self.core.available = False
        challenge = self.start()
        with self.assertRaises(CoreUnavailable):
            self.complete(challenge)
        state = self.store.read_challenge(challenge["challengeId"])
        self.assertIsNone(state["pending"]["photo"])

    def test_expiry_during_inference_never_reaches_core_result(self):
        challenge = self.start()
        analyze = self.analyzer.analyze

        def slow_analyze(content):
            self.now += 91
            return analyze(content)

        with patch.object(self.analyzer, "analyze", side_effect=slow_analyze):
            with self.assertRaises(WorkflowError) as error:
                self.frame(challenge, "CENTER")
        self.assertEqual(error.exception.status_code, 410)
        self.assertEqual(self.core.results, [])
        self.assertEqual(self.notifications(), [])
        self.assertEqual(self.store.read_challenge(challenge["challengeId"])["frames"], 0)

    def test_core_expiry_must_be_a_finite_number(self):
        for expiry in (float("inf"), float("nan"), "tomorrow", True, 10 ** 1000):
            with self.subTest(expiry=expiry), patch.object(self.core, "validate_session", return_value={
                "accepted": True, "expiresAt": expiry,
            }), self.assertRaises(CoreUnavailable):
                self.start()

    def test_core_admin_id_must_fit_java_long_and_postgresql_bigint(self):
        for admin_id in (True, "7", 0, -1, 2 ** 63):
            with self.subTest(admin_id=admin_id), patch.object(self.core, "validate_session", return_value={
                "accepted": True, "expiresAt": 2000, "adminId": admin_id,
            }), self.assertRaises(CoreUnavailable):
                self.start("ENROLL")


if __name__ == "__main__":
    unittest.main()
