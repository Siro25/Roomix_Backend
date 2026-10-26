"""Standalone Telegram photo notifier."""
import json
import os
from datetime import datetime
import cv2
import requests

class TelegramNotifier:
    def __init__(self, bot_token, chat_id, enabled=True):
        self.bot_token = bot_token
        self.chat_id = chat_id
        self.enabled = enabled

    @classmethod
    def from_config(cls, path):
        config = {}
        if os.path.exists(path):
            with open(path, encoding="utf-8") as f:
                config = json.load(f)
        return cls(
            os.environ.get("TELEGRAM_BOT_TOKEN", config.get("telegram_bot_token", "")),
            os.environ.get("TELEGRAM_CHAT_ID", config.get("telegram_chat_id", "")),
            config.get("enable_notification", True),
        )

    def send_alert(self, message, frame):
        if not self.enabled or not self.bot_token or not self.chat_id:
            return False
        ok, jpeg = cv2.imencode(".jpg", frame)
        if not ok:
            return False
        try:
            response = requests.post(
                f"https://api.telegram.org/bot{self.bot_token}/sendPhoto",
                data={"chat_id": self.chat_id, "caption": message + "\n" + datetime.now().isoformat(timespec="seconds")},
                files={"photo": ("alert.jpg", jpeg.tobytes(), "image/jpeg")},
                timeout=10,
            )
            response.raise_for_status()
            return response.json().get("ok") is True
        except (requests.RequestException, ValueError):
            print("[Telegram] Photo alert failed")
            return False
