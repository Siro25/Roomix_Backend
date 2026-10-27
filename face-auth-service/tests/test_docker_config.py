import os
import unittest
from unittest.mock import patch
from urllib.parse import urlsplit, unquote
from src.face_auth.config import Settings


class DockerConfigTests(unittest.TestCase):
    def environment(self, **extra):
        return dict(CORE_URL="http://core-service:8081", FACE_SERVICE_TOKEN="test-only-token",
                    FACE_DB_HOST="face-db", FACE_DB_USER="face_auth",
                    FACE_DB_PASSWORD="test-only:@/# password", **extra)

    def test_separate_fields_encode_password_without_changing_it(self):
        with patch.dict(os.environ, self.environment(), clear=True):
            url = urlsplit(Settings.from_env().database_url)
        self.assertEqual(url.hostname, "face-db")
        self.assertEqual(unquote(url.password), "test-only:@/# password")
        self.assertEqual(url.path, "/roomix_face")

    def test_existing_local_url_keeps_priority(self):
        url = "postgresql://local:password@localhost/local_db"
        with patch.dict(os.environ, self.environment(FACE_DATABASE_URL=url), clear=True):
            self.assertEqual(Settings.from_env().database_url, url)

    def test_invalid_port_is_rejected_without_echoing_credentials(self):
        with patch.dict(os.environ, self.environment(FACE_DB_PORT="not-a-port"), clear=True):
            with self.assertRaisesRegex(ValueError, "FACE_DB_PORT"):
                Settings.from_env()

    def test_missing_password_is_rejected(self):
        env = self.environment()
        env["FACE_DB_PASSWORD"] = ""
        with patch.dict(os.environ, env, clear=True):
            with self.assertRaises(ValueError):
                Settings.from_env()

    def test_host_cannot_inject_url_credentials_or_whitespace(self):
        for host in ("face-db/other", "other@face-db", "face-db\n"):
            env = self.environment()
            env["FACE_DB_HOST"] = host
            with patch.dict(os.environ, env, clear=True):
                with self.assertRaises(ValueError):
                    Settings.from_env()
