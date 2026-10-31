"""Hợp đồng đề xuất cho Core. Không tự giả lập quyền ADMIN khi Core lỗi."""
import httpx


class CoreUnavailable(Exception):
    pass


class CoreClient:
    def __init__(self, settings):
        self.client = httpx.Client(
            base_url=settings.core_url,
            headers={"X-Service-Token": settings.service_token},
            timeout=10,
        )

    def post(self, path, payload, authorization=None):
        headers = {"Authorization": authorization} if authorization else {}
        try:
            response = self.client.post(path, json=payload, headers=headers)
            if response.status_code in (401, 403, 404, 409, 410):
                return {"accepted": False}
            response.raise_for_status()
            result = response.json()
            if not isinstance(result, dict) or type(result.get("accepted")) is not bool:
                raise ValueError("Invalid Core response")
            return result
        except (httpx.HTTPError, ValueError) as exc:
            raise CoreUnavailable("CORE_UNAVAILABLE") from exc

    def validate_session(self, session_id, purpose, authorization):
        return self.post("/internal/face/sessions/validate", {
            "authSessionId": session_id, "purpose": purpose,
        }, authorization)

    def submit_result(self, result):
        return self.post("/internal/face/results", result)

    def close(self):
        self.client.close()
