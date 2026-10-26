"""Thử thách quay đầu trực tiếp ở mức MVP, không phải anti-spoofing chuyên dụng."""
import secrets
from src.face_auth.ai.errors import FaceRejected


def create_actions():
    turns = ["TURN_LEFT", "TURN_RIGHT"]
    secrets.SystemRandom().shuffle(turns)
    return ["CENTER", *turns, "CENTER"]


def advance(state, pose, now):
    """Mỗi bước cần hai frame đạt, cách nhau ít nhất 200 ms từ phía server."""
    if now - state.get("lastFrameAt", 0) < 0.2:
        raise FaceRejected("FRAME_TOO_FAST")
    state["lastFrameAt"] = now
    state.setdefault("stepStartedAt", now)
    if now - state["stepStartedAt"] > 12:
        raise FaceRejected("LIVENESS_TIMEOUT")
    action = state["actions"][state["step"]]
    state["hits"] = state.get("hits", 0) + 1 if pose == action else 0
    if state["hits"] >= 2:
        state["step"] += 1
        state["hits"] = 0
        state["stepStartedAt"] = now
    return state["step"] == len(state["actions"])
