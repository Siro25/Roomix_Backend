"""Kiểm tra thử thách chuyển động bằng landmarks, không gọi AI thật."""
import math
import unittest
from src.face_auth.ai.head_pose import classify_pose
from src.face_auth.ai.errors import FaceRejected
from src.face_auth.ai.liveness import advance, create_actions


def points(nose_x=100):
    return [[70, 60], [130, 60], [nose_x, 95], [80, 125], [120, 125]]


class HeadPoseTests(unittest.TestCase):
    def test_center_left_right_and_transition(self):
        for nose, expected in ((100, "CENTER"), (80, "TURN_LEFT"),
                               (120, "TURN_RIGHT"), (110, "TRANSITION")):
            with self.subTest(nose=nose):
                self.assertEqual(classify_pose(points(nose)), expected)

    def test_small_roll_does_not_become_a_head_turn(self):
        angle = math.radians(15)
        rotated = [[x * math.cos(angle) - y * math.sin(angle),
                    x * math.sin(angle) + y * math.cos(angle)] for x, y in points()]
        self.assertEqual(classify_pose(rotated), "CENTER")

    def test_invalid_or_nonfinite_landmarks_are_rejected(self):
        for value in (None, [], [[0, 0]] * 5, points(float("nan")), points(float("inf"))):
            with self.subTest(points=value), self.assertRaises(FaceRejected):
                classify_pose(value)

    def test_excessive_roll_is_rejected(self):
        invalid = points()
        invalid[1][1] += 50
        with self.assertRaisesRegex(FaceRejected, "HEAD_TOO_TILTED"):
            classify_pose(invalid)


class LivenessTests(unittest.TestCase):
    def state(self):
        return {"actions": ["CENTER", "TURN_LEFT", "TURN_RIGHT", "CENTER"], "step": 0, "hits": 0}

    def test_challenge_contains_both_turns_and_starts_ends_center(self):
        actions = create_actions()
        self.assertEqual(actions[0], "CENTER")
        self.assertEqual(actions[-1], "CENTER")
        self.assertEqual(set(actions[1:3]), {"TURN_LEFT", "TURN_RIGHT"})

    def test_two_consecutive_frames_are_required_for_each_action(self):
        state = self.state()
        now = 1000
        for index, action in enumerate(state["actions"]):
            now += 0.3
            self.assertFalse(advance(state, action, now))
            now += 0.3
            self.assertEqual(advance(state, action, now), index == 3)
        self.assertEqual(state["step"], 4)

    def test_wrong_pose_resets_consecutive_hits(self):
        state = self.state()
        advance(state, "CENTER", 1000)
        advance(state, "TURN_LEFT", 1000.3)
        advance(state, "CENTER", 1000.6)
        self.assertEqual(state["step"], 0)
        self.assertEqual(state["hits"], 1)

    def test_fast_frame_cannot_complete_a_step(self):
        state = self.state()
        advance(state, "CENTER", 1000)
        with self.assertRaisesRegex(FaceRejected, "FRAME_TOO_FAST"):
            advance(state, "CENTER", 1000.1)
        self.assertEqual(state["step"], 0)

    def test_step_timeout_is_a_liveness_failure(self):
        state = self.state()
        advance(state, "TURN_LEFT", 1000)
        with self.assertRaisesRegex(FaceRejected, "LIVENESS_TIMEOUT"):
            advance(state, "CENTER", 1013)

    def test_completing_step_restarts_its_timer(self):
        state = self.state()
        advance(state, "CENTER", 1000)
        advance(state, "CENTER", 1011)
        advance(state, "TURN_LEFT", 1013)
        self.assertEqual(state["step"], 1)
        self.assertEqual(state["hits"], 1)


if __name__ == "__main__":
    unittest.main()
