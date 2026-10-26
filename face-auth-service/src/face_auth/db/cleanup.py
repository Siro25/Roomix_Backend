"""Xóa dữ liệu tạm hết hạn; không xóa hồ sơ khuôn mặt."""
import os
import time
from src.face_auth.db.storage import Store


def cleanup(store, now):
    return store.cleanup_expired(now)


if __name__ == "__main__":
    database = Store(os.environ.get("FACE_DATABASE_URL", ""))
    print(f"Đã dọn {cleanup(database, time.time())} bản ghi tạm hết hạn")
