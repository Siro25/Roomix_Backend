"""Photo alerts on the first unknown-face detection."""
import json
import os
from datetime import datetime
import cv2
import httpx

class SecurityService:
    def __init__(self, config_path=None):
        self.config_path = config_path
        self.config = {"telegram_bot_token": "", "telegram_chat_id": "", "enable_notification": True}
        if config_path and os.path.exists(config_path):
            with open(config_path, encoding="utf-8") as f:
                loaded = json.load(f)
            self.config.update({k: v for k, v in loaded.items() if k in self.config})
        for key in ("telegram_bot_token", "telegram_chat_id"):
            self.config[key] = os.environ.get(key.upper(), self.config[key])
        self._deny_count = 0
        self._http_client = httpx.AsyncClient(timeout=10.0)

    @property
    def deny_count(self):
        return self._deny_count

    def update_config(self, config):
        self.config.update({k: v for k, v in config.items() if k in self.config})
        if self.config_path:
            with open(self.config_path, "w", encoding="utf-8") as f:
                json.dump(self.config, f, ensure_ascii=False, indent=4)

    def on_access_granted(self):
        self._deny_count = 0

    async def on_access_deny(self, frame):
        self._deny_count += 1
        sent = await self._send_telegram(frame) if self.config["enable_notification"] else False
        return {"action": "notification_sent" if sent else "notification_not_sent", "count": self._deny_count}

    async def _send_telegram(self, frame):
        token, chat_id = self.config["telegram_bot_token"], self.config["telegram_chat_id"]
        if not token or not chat_id:
            return False
        ok, jpeg = cv2.imencode(".jpg", frame)
        if not ok:
            return False
        caption = "🚨 Phát hiện khuôn mặt chưa đăng ký!\n" + datetime.now().strftime("%H:%M:%S %d/%m/%Y")
        try:
            response = await self._http_client.post(
                f"https://api.telegram.org/bot{token}/sendPhoto",
                data={"chat_id": chat_id, "caption": caption},
                files={"photo": ("alert.jpg", jpeg.tobytes(), "image/jpeg")},
            )
            response.raise_for_status()
            return response.json().get("ok") is True
        except (httpx.HTTPError, ValueError):
            print("[Telegram] Photo alert failed")
            return False

    async def close(self):
        await self._http_client.aclose()
