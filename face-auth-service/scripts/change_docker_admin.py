"""Change only the saved manual Docker test Admin; never expose the password.

Run from a real terminal with .venv-runtime/Scripts/python.exe.
--check-only reads the target and checks BCrypt, without changing any account.
"""
import argparse
import getpass
import hmac
import json
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys
import uuid


FACE = Path(__file__).resolve().parents[1]


def command(args, content=None):
    result = subprocess.run(args, input=content, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, timeout=60)
    if result.returncode:
        # Do not echo SQL, hashes, process arguments or stderr into the terminal.
        raise RuntimeError("Lenh that bai. Kiem tra Docker/JDK va username trung; khong co secret duoc in ra.")
    return result.stdout.decode("utf-8").strip()


def hash_password(password):
    java = shutil.which("java")
    fallback = Path(r"C:\Program Files\Java\jdk-21.0.12\bin\java.exe")
    if fallback.is_file():
        java = str(fallback)
    if not java:
        raise RuntimeError("Can JDK 21 de dung BCrypt cua Core.")
    jars = sorted((Path.home()/".m2/repository/org/springframework/security/spring-security-crypto").glob("*/*.jar"))
    jars = [p for p in jars if not p.name.endswith(("-sources.jar", "-javadoc.jar"))]
    if not jars:
        raise RuntimeError("Chua co spring-security-crypto trong Maven cache cua Core.")
    encoded = command([java, "-cp", str(jars[-1]), str(FACE/"scripts/HashAdminPassword.java")],
                      password.encode("utf-8"))
    if not re.fullmatch(r"\$2[aby]\$12\$[./A-Za-z0-9]{53}", encoded):
        raise RuntimeError("BCrypt khong tra ve hash hop le. Chua doi tai khoan.")
    return encoded


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check-only", action="store_true")
    options = parser.parse_args()
    statefile = FACE/".tmp/docker-webcam-state.json"
    state = json.loads(statefile.read_text(encoding="utf-8-sig"))
    project = state["project"]
    if not re.fullmatch(r"roomix_admin_smoke_[0-9a-f]{10}", project):
        raise RuntimeError("Khong phai bo Docker webcam da luu. Khong cap nhat.")
    admin_id = str(uuid.UUID(state["admin_id"]))
    containers = command(["docker", "ps", "-q", "--filter", "label=com.docker.compose.project="+project,
                          "--filter", "label=com.docker.compose.service=core-db"]).splitlines()
    if len(containers) != 1:
        raise RuntimeError("Hay bat nhom Docker webcam truoc; can dung mot core-db dang chay.")
    db_command = ["docker", "exec", "-i", containers[0], "psql", "-X", "-qAt",
                  "-v", "ON_ERROR_STOP=1", "-U", "postgres", "-d", "core_db"]

    def sql(statement):
        return command(db_command, statement.encode("utf-8"))

    data = sql(f"SELECT json_build_object('id',id,'username',username,'role',role,'status',status) FROM users WHERE id='{admin_id}'::uuid;")
    account = json.loads(data) if data else None
    if not account or account["role"] != "ADMIN" or account["status"] != "ACTIVE":
        raise RuntimeError("Tai khoan dich khong phai ADMIN ACTIVE. Khong cap nhat.")
    # Confirm Java/BCrypt works before accepting a real password or doing any write.
    hash_password(secrets.token_urlsafe(24))
    print("Tai khoan Docker hien tai:", account["username"])
    if options.check_only:
        print("CHECK_OK: dung Admin, database Docker va BCrypt. Khong thay doi du lieu.")
        return
    if not sys.stdin.isatty():
        raise RuntimeError("Chay truc tiep trong PowerShell de nhap mat khau an; khong pipe mat khau.")
    username = input("Username moi (4-50 ky tu A-Z/a-z/0-9/._-): ").strip()
    if not re.fullmatch(r"[A-Za-z0-9._-]{4,50}", username):
        raise RuntimeError("Username khong hop le. Chua thay doi.")
    password = getpass.getpass("Mat khau moi (an khi nhap): ")
    confirm = getpass.getpass("Nhap lai mat khau moi: ")
    if not hmac.compare_digest(password.encode("utf-8"), confirm.encode("utf-8")):
        raise RuntimeError("Hai mat khau khong trung. Chua thay doi.")
    if password.isspace() or not 8 <= len(password.encode("utf-8")) <= 72:
        raise RuntimeError("Mat khau can 8-72 byte UTF-8. Chua thay doi.")
    hashed = hash_password(password)
    del password, confirm
    # Both interpolated values are strictly validated above. A single UPDATE is
    # atomic: a duplicate username rolls the whole statement back.
    result = sql(f"""UPDATE users SET username='{username}', password_hash='{hashed}',
        updated_at=CURRENT_TIMESTAMP
        WHERE id='{admin_id}'::uuid AND role='ADMIN' AND status='ACTIVE'
        RETURNING json_build_object('id',id,'username',username,'saved',password_hash='{hashed}');""")
    del hashed
    saved = json.loads(result) if result else None
    if not saved or saved["id"] != admin_id or not saved["saved"]:
        raise RuntimeError("Khong nhan duoc xac nhan cap nhat. Kiem tra tai khoan truoc khi thu lai.")
    try:
        (FACE/".tmp/docker-webcam-access.txt").write_text(
            "LOCAL ADMIN ACCESS - DO NOT COMMIT\nUsername: "+username+
            "\nPassword: changed by owner; deliberately not stored here.\n"
            "Admin ID unchanged. Previously enrolled face is retained.\n", encoding="utf-8")
    except OSError:
        print("Luu y: database da doi, nhung file ghi chu local chua cap nhat duoc.")
    print("DA DOI THANH CONG. Username moi:", username)
    print("Dang xuat, dang nhap lai bang mat khau moi. ID va khuon mat duoc giu nguyen.")
    print("Mat khau moi khong luu trong docker-webcam-access.txt. Hay tu ghi nho.")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError, RuntimeError, subprocess.TimeoutExpired) as error:
        # Known messages contain no user-entered values or hashes.
        if isinstance(error, RuntimeError):
            print("LOI:", error)
        else:
            print("LOI: Khong thuc hien duoc. Kiem tra Docker, file trang thai va JDK.")
        raise SystemExit(1)
    except (KeyboardInterrupt, EOFError):
        print("\nDa dung. Neu chua hien DA DOI THANH CONG, kiem tra tai khoan truoc khi thu lai.")
        raise SystemExit(1)
