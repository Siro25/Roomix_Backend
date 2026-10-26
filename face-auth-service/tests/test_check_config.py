"""Chẩn đoán không được làm lộ URL/mật khẩu khi PostgreSQL gặp lỗi."""
import io
import unittest
from contextlib import redirect_stdout
from unittest.mock import patch

import psycopg

from src.face_auth.check_config import main
from src.face_auth.config import Settings


class CheckConfigTests(unittest.TestCase):
    def settings(self, **extra):
        return Settings(core_url="http://localhost:8081", service_token="secret",
                        database_url="postgresql://user:private@localhost/face", **extra)

    def test_success_only_checks_database_connection(self):
        with patch("src.face_auth.check_config.Settings.from_env", return_value=self.settings()), \
                patch("src.face_auth.check_config.psycopg.connect") as connect, \
                redirect_stdout(io.StringIO()):
            self.assertEqual(main(), 0)
        connect.return_value.__enter__.return_value.execute.assert_called_once_with("SELECT 1")

    def test_database_error_does_not_print_connection_secrets(self):
        output = io.StringIO()
        with patch("src.face_auth.check_config.Settings.from_env", return_value=self.settings()), \
                patch("src.face_auth.check_config.psycopg.connect",
                      side_effect=psycopg.OperationalError("private password and URL")), \
                redirect_stdout(output):
            self.assertEqual(main(), 1)
        self.assertIn("DATABASE_UNAVAILABLE", output.getvalue())
        self.assertNotIn("private", output.getvalue())

    def test_partial_telegram_configuration_stops_before_database(self):
        with patch("src.face_auth.check_config.Settings.from_env",
                   return_value=self.settings(telegram_token="secret")), \
                patch("src.face_auth.check_config.psycopg.connect") as connect, \
                redirect_stdout(io.StringIO()):
            self.assertEqual(main(), 1)
        connect.assert_not_called()

    def test_invalid_settings_stop_before_database(self):
        with patch("src.face_auth.check_config.Settings.from_env", side_effect=ValueError("missing config")), \
                patch("src.face_auth.check_config.psycopg.connect") as connect, \
                redirect_stdout(io.StringIO()):
            self.assertEqual(main(), 1)
        connect.assert_not_called()
