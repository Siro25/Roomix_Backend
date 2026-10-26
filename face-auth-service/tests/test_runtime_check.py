"""Test lệnh chẩn đoán không tải mô hình trong bộ unit test."""
import io
import json
import unittest
from unittest.mock import patch
from src.face_auth import check_runtime


class RuntimeCheckTests(unittest.TestCase):
    def run_check(self, arguments, versions, model_result=None):
        output = io.StringIO()
        with patch.object(check_runtime, "installed_versions", return_value=versions), \
             patch.object(check_runtime, "check_models", return_value=model_result) as model, \
             patch("sys.stdout", output):
            code = check_runtime.main(arguments)
        return code, json.loads(output.getvalue()), model

    def test_missing_dependency_fails_without_loading_models(self):
        code, report, model = self.run_check(["--models"], {"torch": None})
        self.assertEqual(code, 1)
        self.assertEqual(report["missing"], ["torch"])
        model.assert_not_called()

    def test_default_check_does_not_download_or_load_models(self):
        code, report, model = self.run_check([], {"torch": "installed"})
        self.assertEqual(code, 0)
        self.assertEqual(report["status"], "OK")
        model.assert_not_called()

    def test_explicit_models_check_reports_inference(self):
        code, report, model = self.run_check(["--models"], {"torch": "installed"}, {"models": "OK"})
        self.assertEqual(code, 0)
        self.assertEqual(report["inference"]["models"], "OK")
        model.assert_called_once_with(None)

    def test_failure_does_not_print_exception_with_secrets(self):
        output = io.StringIO()
        with patch.object(check_runtime, "installed_versions", return_value={}), \
             patch.object(check_runtime, "check_models", side_effect=RuntimeError("password=secret")), \
             patch("sys.stdout", output):
            self.assertEqual(check_runtime.main(["--models"]), 1)
        report = json.loads(output.getvalue())
        self.assertEqual(report["status"], "MODELS_FAILED")
        self.assertNotIn("secret", output.getvalue())


if __name__ == "__main__":
    unittest.main()
