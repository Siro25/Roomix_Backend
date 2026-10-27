"""Đọc cấu hình từ biến môi trường; không ghi bí mật vào mã nguồn."""
import os
import re
from urllib.parse import quote
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    core_url: str
    service_token: str
    database_url: str = ""
    session_seconds: int = 90
    match_threshold: float = 0.80
    match_margin: float = 0.05
    telegram_token: str = ""
    telegram_chat_id: str = ""
    enable_demo: bool = False

    @classmethod
    def from_env(cls):
        core_url = os.environ.get("CORE_URL", "").rstrip("/")
        token = os.environ.get("FACE_SERVICE_TOKEN", "")
        database_url = os.environ.get("FACE_DATABASE_URL", "")
        # Docker passes database fields separately so passwords containing @, /,
        # or # are encoded safely. Existing local URL configuration keeps priority.
        if not database_url and os.environ.get("FACE_DB_HOST"):
            host = os.environ["FACE_DB_HOST"]
            port = os.environ.get("FACE_DB_PORT", "5432")
            user = os.environ.get("FACE_DB_USER", "")
            password = os.environ.get("FACE_DB_PASSWORD", "")
            name = os.environ.get("FACE_DB_NAME", "roomix_face")
            if not re.fullmatch(r"[A-Za-z0-9.-]+", host) or not user or not password or not name:
                raise ValueError("Cấu hình PostgreSQL Face chưa hợp lệ")
            if not port.isdigit() or not 1 <= int(port) <= 65535:
                raise ValueError("FACE_DB_PORT chưa hợp lệ")
            database_url = (f"postgresql://{quote(user, safe='')}:{quote(password, safe='')}"
                            f"@{host}:{port}/{quote(name, safe='')}")
        if not core_url or not token:
            raise ValueError("Cần cấu hình CORE_URL và FACE_SERVICE_TOKEN")
        if not core_url.startswith(("http://", "https://")):
            raise ValueError("CORE_URL phải là địa chỉ HTTP hoặc HTTPS")
        if not database_url.startswith(("postgresql://", "postgres://")):
            raise ValueError("Cần cấu hình FACE_DATABASE_URL bằng URL PostgreSQL")
        return cls(
            core_url=core_url,
            service_token=token,
            database_url=database_url,
            telegram_token=os.environ.get("TELEGRAM_BOT_TOKEN", ""),
            telegram_chat_id=os.environ.get("TELEGRAM_CHAT_ID", ""),
            enable_demo=os.environ.get("FACE_ENABLE_DEMO", "false").lower() == "true",
        )
