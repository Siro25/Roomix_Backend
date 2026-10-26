"""Điều phối luồng Face Auth. Core luôn là nơi quyết định đăng nhập."""
import base64
import secrets
import time
import math
from src.face_auth.admin_identity import validate_admin_id
from src.face_auth.integrations.core_client import CoreUnavailable
from src.face_auth.ai.errors import FaceRejected
from src.face_auth.ai.liveness import advance, create_actions
from src.face_auth.ai.matching import identify, mean_vector, similarity
from src.face_auth.db.notification_store import NotificationQueue


class WorkflowError(Exception):
    def __init__(self, status_code, reason):
        self.status_code = status_code
        self.reason = reason


class FaceWorkflow:
    def __init__(self, settings, store, core, analyzer, clock=time.time):
        self.settings = settings
        self.store = store
        self.core = core
        self.analyzer = analyzer
        self.clock = clock

    def start(self, session_id, purpose, authorization):
        if purpose == "ENROLL" and not authorization:
            raise WorkflowError(401, "ADMIN_LOGIN_REQUIRED")
        permission = self.core.validate_session(session_id, purpose, authorization)
        if permission.get("accepted") is not True:
            raise WorkflowError(403, "CORE_SESSION_REJECTED")
        admin_id = permission.get("adminId")
        if purpose == "ENROLL":
            try:
                admin_id = validate_admin_id(admin_id)
            except ValueError as exc:
                raise CoreUnavailable("CORE_INVALID_ADMIN_ID") from exc
        core_expiry = permission.get("expiresAt")
        if (type(core_expiry) not in (int, float) or abs(core_expiry) > 2 ** 63 - 1
                or not math.isfinite(core_expiry)):
            raise CoreUnavailable("CORE_INVALID_EXPIRY")
        if not self.clock() < core_expiry:
            raise WorkflowError(403, "CORE_SESSION_EXPIRED")
        challenge_id = secrets.token_urlsafe(32)
        state = {
            "authSessionId": session_id, "purpose": purpose,
            "adminId": admin_id if purpose == "ENROLL" else None,
            "expiresAt": min(core_expiry, self.clock() + self.settings.session_seconds),
            "actions": create_actions(), "step": 0, "hits": 0,
            "frames": 0, "vectors": [], "status": "ACTIVE",
        }
        try:
            self.store.create_challenge(challenge_id, state)
        except ValueError as exc:
            raise WorkflowError(409, str(exc)) from exc
        return {"challengeId": challenge_id, "actions": state["actions"], "expiresAt": state["expiresAt"]}

    def process_frame(self, challenge_id, content):
        core_error = None
        try:
            with self.store.edit_challenge(challenge_id) as (db, state):
                if state["status"] == "DONE":
                    raise WorkflowError(409, "CHALLENGE_ALREADY_USED")
                if self.clock() >= state["expiresAt"]:
                    raise WorkflowError(410, "CHALLENGE_EXPIRED")
                if "pending" not in state:
                    self._inspect_frame(db, state, content)
                # Inference có thể chậm: kiểm tra lại trước khi báo kết quả cho Core.
                if self.clock() >= state["expiresAt"]:
                    raise WorkflowError(410, "CHALLENGE_EXPIRED")
                if "pending" in state:
                    try:
                        response = self._finish(db, state)
                    except CoreUnavailable as exc:
                        # Lưu kết quả và ảnh đúng phiên để retry, không nhận mặt lại.
                        core_error = exc
                else:
                    response = {"status": "IN_PROGRESS", "action": state["actions"][state["step"]]}
        except KeyError as exc:
            raise WorkflowError(404, "CHALLENGE_NOT_FOUND") from exc
        if core_error:
            raise core_error
        return response

    def status(self, challenge_id):
        """Đọc kết quả khi trình duyệt mất response cuối, không xử lý mặt lại."""
        try:
            state = self.store.read_challenge(challenge_id)
        except KeyError as exc:
            raise WorkflowError(404, "CHALLENGE_NOT_FOUND") from exc
        if state["status"] == "DONE":
            return {"status": state["result"], "reason": state.get("reason")}
        if self.clock() >= state["expiresAt"]:
            raise WorkflowError(410, "CHALLENGE_EXPIRED")
        if "pending" in state:
            return {"status": "IN_PROGRESS", "reason": "CORE_PENDING"}
        return {"status": "IN_PROGRESS", "action": state["actions"][state["step"]]}

    def _inspect_frame(self, db, state, content):
        state["frames"] += 1
        photo = None
        reason = None
        try:
            observation = self.analyzer.analyze(content)
            photo = observation.photo
            if state["frames"] >= 120:
                raise FaceRejected("LIVENESS_FAILED", photo)
            if "reference" in state and similarity(state["reference"], observation.embedding) < 0.65:
                raise FaceRejected("FACE_CHANGED")
            expected_action = state["actions"][state["step"]]
            if observation.pose == "CENTER" and expected_action == "CENTER":
                state.setdefault("reference", observation.embedding)
                state["vectors"].append(observation.embedding)
            completed = advance(state, observation.pose, self.clock())
            if not completed:
                return
        except FaceRejected as exc:
            reason = str(exc)
            photo = exc.photo or photo
            if reason == "FRAME_TOO_FAST":
                raise WorkflowError(429, reason) from exc

        vector = mean_vector(state["vectors"]) if reason is None else None
        admin_id = state["adminId"]
        profiles = self.store.profiles(db)
        if reason is None and state["purpose"] == "LOGIN":
            admin_id, reason = identify(vector, profiles, self.settings.match_threshold, self.settings.match_margin)
        if reason is None and state["purpose"] == "ENROLL":
            if any(other != admin_id and similarity(vector, known) >= self.settings.match_threshold
                   for other, known in profiles.items()):
                reason = "FACE_ALREADY_ENROLLED"

        state["pending"] = {
            "result": {
                "authSessionId": state["authSessionId"], "purpose": state["purpose"],
                "verified": reason is None, "adminId": admin_id if reason is None else None,
                "reason": reason,
            },
            "vector": vector,
            "photo": base64.b64encode(photo).decode("ascii")
                if photo and self.settings.telegram_token and self.settings.telegram_chat_id else None,
        }

    def _finish(self, db, state):
        pending = state["pending"]
        result = pending["result"]
        decision = self.core.submit_result(result)
        succeeded = result["verified"] and decision.get("accepted") is True
        reason = result["reason"] or (None if succeeded else "CORE_REJECTED")
        if succeeded and state["purpose"] == "ENROLL":
            self.store.save_profile(db, state["adminId"], pending["vector"], self.clock())
        event = None
        if state["purpose"] == "LOGIN" and not succeeded:
            event = "FACE_LOGIN_FAILED"
        elif state["purpose"] == "ENROLL" and succeeded:
            event = "FACE_ENROLLED"
        if event and pending["photo"] and self.settings.telegram_token and self.settings.telegram_chat_id:
            NotificationQueue.enqueue(db, event, state["authSessionId"],
                base64.b64decode(pending["photo"]), result["adminId"], reason, self.clock())
        # Xóa ảnh và embedding tạm sau khi có kết quả cuối cùng.
        state.clear()
        state.update({"status": "DONE", "result": "SUCCEEDED" if succeeded else "FAILED", "reason": reason})
        return {"status": state["result"], "reason": reason}
