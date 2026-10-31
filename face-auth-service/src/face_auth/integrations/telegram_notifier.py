"""Chỉ gửi ảnh khi đăng nhập mặt thất bại hoặc đăng ký mặt thành công."""
import logging
from datetime import datetime, timezone
import httpx

logger = logging.getLogger(__name__)


class TelegramNotifier:
    def __init__(self, settings):
        self.token = settings.telegram_token
        self.chat_id = settings.telegram_chat_id
        self.client = httpx.Client(timeout=10)

    def send(self, event, event_id, photo, admin_id=None, *, reason=None, occurred_at=None):
        if event not in ("FACE_LOGIN_FAILED", "FACE_ENROLLED"):
            return False
        if not self.token or not self.chat_id:
            return False
        title = "Đăng nhập khuôn mặt thất bại" if event == "FACE_LOGIN_FAILED" else "Đăng ký khuôn mặt thành công"
        timestamp = occurred_at or datetime.now(timezone.utc)
        caption = (f"Roomix | {title}\nAdmin: {admin_id if admin_id is not None else 'Unknown'}"
                   f"\nEvent: {event_id}\nTime: {timestamp.isoformat()}")
        if reason:
            caption += f"\nReason: {reason}"
        try:
            response = self.client.post(
                f"https://api.telegram.org/bot{self.token}/sendPhoto",
                data={"chat_id": self.chat_id, "caption": caption},
                files={"photo": ("face.jpg", photo, "image/jpeg")},
            )
            response.raise_for_status()
            return response.json().get("ok") is True
        except (httpx.HTTPError, ValueError):
            # Không log URL vì URL Telegram chứa bot token.
            logger.warning("Không gửi được ảnh Telegram")
            return False

    def close(self):
        self.client.close()
