"""Test Core/Gateway/Face using the team's Compose, isolated data and ports.

Run from face-auth-service: .venv-runtime/Scripts/python.exe scripts/check_compose.py
Requires the three local test images described in docs/COMPOSE_TEST.md.
"""
import base64
import io
import json
import os
from pathlib import Path
import re
import secrets
import subprocess
import time
import uuid

import httpx
from PIL import Image

FACE = Path(__file__).resolve().parents[1]
REPO = FACE.parent
# Disposable test credentials, never used in the user's database or normal startup.
PASSWORD = "Compose-test-only-password-123!"
PASSWORD_HASH = "$2a$12$sJUOPaoRkva4eVJQYU.AWOEmQZoAw/K98M3O3.7I8t9XfVxRHJJXe"
ADMIN_ID = "a0000000-0000-4000-8000-000000000001"
TENANT_ID = "a0000000-0000-4000-8000-000000000002"


class ComposeTest:
    def __init__(self):
        self.project = "roomix_admin_smoke_" + uuid.uuid4().hex[:10]
        self.folder = FACE / ".tmp" / self.project
        self.folder.mkdir(parents=True)
        self.override = self.folder / "override.yml"
        self.fixture = self.folder / "fixture.yml"
        self.env_file = self.folder / "test.env"
        self.environment = os.environ.copy()
        self.environment.update({
            "ROOMIX_DB_PASSWORD": secrets.token_urlsafe(24),
            "ROOMIX_JWT_SECRET": base64.b64encode(secrets.token_bytes(32)).decode(),
            "FACE_SERVICE_TOKEN": secrets.token_hex(32),
            "TELEGRAM_BOT_TOKEN": "", "TELEGRAM_CHAT_ID": "",
            "CLOUDINARY_CLOUD_NAME": "", "CLOUDINARY_API_KEY": "", "CLOUDINARY_API_SECRET": "",
            "CORE_TELEGRAM_WORKER_ENABLED": "false",
        })
        self.env_file.write_text("# Test values are supplied by the test process, not the real .env.\n", encoding="utf-8")
        self.started = False

    def command(self, *args, fixture=False, timeout=300, input_data=None):
        command = ["docker", "compose", "--env-file", str(self.env_file), "-p", self.project,
                   "-f", str(REPO / "compose.yml"), "-f", str(self.override)]
        if fixture:
            command += ["-f", str(self.fixture)]
        result = subprocess.run(command + list(args), cwd=REPO, env=self.environment,
                                input=input_data, capture_output=True, text=True, timeout=timeout)
        with (self.folder / "compose.log").open("a", encoding="utf-8") as log:
            # Do not log commands, env values, SQL, config JSON or HTTP responses.
            if args[0] in ("up", "down"):
                log.write(result.stdout + result.stderr)
        if result.returncode:
            raise RuntimeError("Compose operation failed: " + args[0] + "; inspect the local compose.log")
        return result.stdout

    def prepare(self):
        model_path = json.dumps(str(FACE / "models"))
        fixture_path = json.dumps(str(FACE / "tests" / "compose"))
        self.override.write_text("""services:
  core-db:
    ports: !reset []
    restart: "no"
  face-db:
    restart: "no"
  kafka:
    ports: !reset []
    environment:
      KAFKA_HEAP_OPTS: "-Xms256m -Xmx256m"
    restart: "no"
  core-service:
    image: roomix-core-admin-test:local
    ports: !reset []
    environment:
      TELEGRAM_BOT_TOKEN: ""
      TELEGRAM_CHAT_ID: ""
      CORE_TELEGRAM_WORKER_ENABLED: "false"
      JAVA_TOOL_OPTIONS: "-Xmx384m"
    restart: "no"
  face-auth-service:
    image: roomix-face-auth:local
    ports: !reset []
    environment:
      TELEGRAM_BOT_TOKEN: ""
      TELEGRAM_CHAT_ID: ""
    volumes: !override
      - type: bind
        source: """ + model_path + """
        target: /app/models
        read_only: true
    restart: "no"
  api-gateway:
    image: roomix-gateway-admin-test:local
    ports: !override
      - "127.0.0.1::8080"
    depends_on: !override
      core-service:
        condition: service_started
      face-auth-service:
        condition: service_healthy
    environment:
      JAVA_TOOL_OPTIONS: "-Xmx256m"
    restart: "no"
""", encoding="utf-8")
        self.fixture.write_text("""services:
  face-auth-service:
    environment:
      PYTHONPATH: /test-tools:/app
    volumes:
      - type: bind
        source: """ + fixture_path + """
        target: /test-tools
        read_only: true
    command: [python, -m, uvicorn, compose_face_fixture:app, --host, 0.0.0.0, --port, "8084", --no-access-log]
""", encoding="utf-8")
        configuration = json.loads(self.command("config", "--format", "json"))
        for volume in configuration["volumes"].values():
            assert not volume.get("external") and volume["name"].startswith(self.project + "_")
        assert set(configuration["services"]["api-gateway"]["depends_on"]) == {"core-service", "face-auth-service"}
        print("ISOLATION_OK: unique Compose project, temporary volumes, random Gateway port, Telegram disabled", flush=True)

    def sql(self, service, database, user, query):
        # Pass controlled test SQL via stdin; never use the real database connection.
        command = ["docker", "compose", "--env-file", str(self.env_file), "-p", self.project,
                   "-f", str(REPO / "compose.yml"), "-f", str(self.override), "exec", "-T", service,
                   "psql", "-U", user, "-d", database, "-v", "ON_ERROR_STOP=1", "-At"]
        result = subprocess.run(command, cwd=REPO, env=self.environment, input=query,
                                capture_output=True, text=True, timeout=20)
        if result.returncode:
            raise RuntimeError("Test SQL failed in " + service)
        return result.stdout.strip()

    def seed(self):
        self.sql("core-db", "core_db", "postgres", f"""
            INSERT INTO users(id,username,email,password_hash,full_name,phone_number,role,status,created_at,updated_at)
            VALUES ('{ADMIN_ID}','compose_admin','compose-admin@example.test','{PASSWORD_HASH}','Compose Admin','','ADMIN','ACTIVE',now(),now()),
                   ('{TENANT_ID}','compose_tenant','compose-tenant@example.test','{PASSWORD_HASH}','Compose Tenant','','TENANT','ACTIVE',now(),now());
        """)
        assert self.sql("core-db", "core_db", "postgres", "SELECT count(*) FROM flyway_schema_history WHERE success;") == "13"

    @staticmethod
    def request(client, method, path, expected=200, **kwargs):
        response = client.request(method, path, **kwargs)
        assert response.status_code == expected, f"{method} {path}: expected {expected}, got {response.status_code}"
        return response.json()

    def wait_for_api(self, client):
        deadline = time.monotonic() + 90
        while True:
            try:
                if client.get("/openapi/core/v3/api-docs").status_code == 200:
                    return
            except httpx.TransportError:
                pass
            if time.monotonic() > deadline:
                raise RuntimeError("Gateway/Core did not become ready")
            time.sleep(.5)

    def create_ticket(self, client, purpose="LOGIN", headers=None):
        return self.request(client, "POST", "/api/auth/face/sessions", json={"purpose": purpose}, headers=headers)

    def create_challenge(self, client, ticket, headers=None):
        return self.request(client, "POST", "/api/face/challenges", json={
            "authSessionId": ticket["authSessionId"], "purpose": ticket["purpose"]}, headers=headers)

    def complete(self, client, ticket, expected=200):
        return self.request(client, "POST", "/api/auth/face/complete", expected, json={
            "authSessionId": ticket["authSessionId"], "completionToken": ticket["completionToken"]})

    def fixture_frames(self, client, challenge, identity="FIRST"):
        result = None
        for action in challenge["actions"]:
            for _ in range(2):
                time.sleep(.25)
                result = self.request(client, "POST", "/api/face/challenges/" + challenge["challengeId"] + "/frames",
                                      content=(identity + ":" + action).encode(), headers={"Content-Type": "image/jpeg"})
        return result

    def run(self):
        self.prepare()
        self.started = True
        self.command("up", "--detach", "--no-build", "--wait", "--wait-timeout", "240",
                     "core-service", "face-auth-service", "api-gateway", timeout=600)
        port = self.command("port", "api-gateway", "8080").strip().rsplit(":", 1)[1]
        with httpx.Client(base_url="http://127.0.0.1:" + port, timeout=25) as client:
            self.wait_for_api(client)
            self.seed()
            login = self.request(client, "POST", "/api/auth/login", json={"username": "compose_admin", "password": PASSWORD})["data"]
            headers = {"Authorization": "Bearer " + login["accessToken"]}
            profile = self.request(client, "GET", "/api/users/me", headers=headers)["data"]
            assert profile["id"] == ADMIN_ID and profile["role"] == "ADMIN"
            self.request(client, "POST", "/api/auth/login", 401, json={"username": "compose_admin", "password": "wrong"})
            self.request(client, "POST", "/api/auth/face/sessions", 401, json={"purpose": "ENROLL"})
            print("PASS: Flyway V1-V13, password login, profile and ENROLL authorization through Gateway", flush=True)

            # Production Face app, real AI, no test fixture mounted yet.
            ticket = self.create_ticket(client)
            challenge = self.create_challenge(client, ticket)
            blank = io.BytesIO()
            Image.new("RGB", (640, 480), "gray").save(blank, format="JPEG")
            rejected = self.request(client, "POST", "/api/face/challenges/" + challenge["challengeId"] + "/frames",
                                    content=blank.getvalue(), headers={"Content-Type": "image/jpeg"})
            assert rejected["status"] == "FAILED" and rejected["reason"] == "NO_FACE"
            self.complete(client, ticket, 409)
            self.request(client, "POST", "/internal/face/results", 404, json={})
            print("PASS: real AI rejects blank image; Face -> Core callback; no JWT for failure; internal routes not exposed", flush=True)

            # Explicit test-only module tests success without using private face images.
            self.command("up", "--detach", "--no-build", "--no-deps", "--force-recreate", "--wait", "--wait-timeout", "90",
                         "face-auth-service", fixture=True, timeout=180)
            ticket = self.create_ticket(client, "ENROLL", headers)
            challenge = self.create_challenge(client, ticket, headers)
            assert self.fixture_frames(client, challenge)["status"] == "SUCCEEDED"
            saved = self.sql("face-db", "roomix_face", "face_auth", "SELECT admin_id || ':' || jsonb_array_length(embedding) FROM face_profiles;")
            assert saved == ADMIN_ID + ":512"
            ticket = self.create_ticket(client)
            challenge = self.create_challenge(client, ticket)
            assert self.fixture_frames(client, challenge)["status"] == "SUCCEEDED"
            result = self.complete(client, ticket)
            assert self.complete(client, ticket)["accessToken"] == result["accessToken"]
            face_headers = {"Authorization": "Bearer " + result["accessToken"]}
            assert self.request(client, "GET", "/api/users/me", headers=face_headers)["data"]["id"] == ADMIN_ID
            self.request(client, "POST", "/api/auth/face/sessions", 403, headers=face_headers, json={"purpose": "ENROLL"})
            print("PASS: fixture ENROLL saves UUID profile; LOGIN issues Core JWT; retry is idempotent; FACE JWT cannot ENROLL", flush=True)

            ticket = self.create_ticket(client)
            assert self.fixture_frames(client, self.create_challenge(client, ticket), "OTHER")["reason"] == "FACE_NOT_RECOGNIZED"
            self.complete(client, ticket, 409)
            tenant = self.request(client, "POST", "/api/auth/login", json={"username": "compose_tenant", "password": PASSWORD})["data"]
            self.request(client, "POST", "/api/auth/face/sessions", 403,
                         headers={"Authorization": "Bearer " + tenant["accessToken"]}, json={"purpose": "ENROLL"})
            self.sql("core-db", "core_db", "postgres", "UPDATE users SET status='LOCKED' WHERE username='compose_admin';")
            self.request(client, "POST", "/api/auth/login", 401, json={"username": "compose_admin", "password": PASSWORD})
            ticket = self.create_ticket(client)
            assert self.fixture_frames(client, self.create_challenge(client, ticket))["reason"] == "CORE_REJECTED"
            self.complete(client, ticket, 409)
            assert self.sql("core-db", "core_db", "postgres", "SELECT count(*) FROM auth_telegram_outbox;") == "0"
            assert self.sql("face-db", "roomix_face", "face_auth", "SELECT count(*) FROM telegram_outbox;") == "0"
            print("PASS: unknown face, Tenant enrollment and locked Admin rejected; no Telegram event/message", flush=True)
        print("COMPOSE_ADMIN_STACK_OK: Core + Gateway + Face + Kafka + two PostgreSQL databases", flush=True)

    def cleanup(self):
        if self.started:
            assert re.fullmatch(r"roomix_admin_smoke_[0-9a-f]{10}", self.project)
            self.command("down", "--volumes", "--remove-orphans", timeout=120)
            print("CLEANED: only the isolated test Compose project/volumes; local services and user databases untouched", flush=True)


if __name__ == "__main__":
    test = ComposeTest()
    try:
        test.run()
    finally:
        test.cleanup()
