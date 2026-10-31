"""Kiểm tra cấu hình và kết nối PostgreSQL, không tạo bảng hoặc nạp AI."""
import psycopg

from src.face_auth.config import Settings


def main():
    try:
        settings = Settings.from_env()
    except ValueError as exc:
        # Settings chỉ dùng thông báo cố định, không chứa giá trị bí mật.
        print(f"CONFIG_ERROR: {exc}")
        return 1

    if bool(settings.telegram_token) != bool(settings.telegram_chat_id):
        print("CONFIG_ERROR: Telegram cần cả TELEGRAM_BOT_TOKEN và TELEGRAM_CHAT_ID")
        return 1

    try:
        with psycopg.connect(settings.database_url, connect_timeout=5) as db:
            db.execute("SELECT 1")
    except psycopg.Error:
        # Không in exception: thông báo driver có thể chứa thông tin kết nối.
        print("DATABASE_UNAVAILABLE: Kiểm tra PostgreSQL, database và tài khoản trong cấu hình.")
        return 1

    print("CONFIG_OK: Đọc được cấu hình và kết nối PostgreSQL.")
    print("Chưa kiểm tra API Core, AI, webcam hoặc gửi Telegram.")
    return 0


if __name__ == "__main__":
    import argparse
    from dotenv import load_dotenv

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--env-file", help="File cấu hình cục bộ; không ghi đè biến môi trường có sẵn.")
    args = parser.parse_args()
    if args.env_file:
        load_dotenv(args.env_file, override=False)
    raise SystemExit(main())
