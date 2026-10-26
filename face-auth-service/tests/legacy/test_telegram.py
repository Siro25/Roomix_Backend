"""Contract tests without downloading AI models or sending real alerts."""
import asyncio
import importlib.util
from pathlib import Path
import sys
import types
import unittest
from unittest.mock import AsyncMock, Mock, patch

ROOT = Path(__file__).resolve().parents[2]

def run_immediate(coro):
    """Drive coroutines whose I/O is fully mocked; no OS event loop needed."""
    try:
        coro.send(None)
    except StopIteration as done:
        return done.value
    raise AssertionError("Unexpected real asynchronous I/O in contract test")

class TelegramTests(unittest.TestCase):
    def setUp(self):
        self.cv = types.ModuleType('cv2')
        self.cv.imencode = Mock(return_value=(True, Mock(tobytes=lambda: b'jpeg-data')))
        self.http = types.ModuleType('httpx')
        self.http.HTTPError = type('HTTPError', (Exception,), {})
        response = Mock()
        response.json.return_value = {'ok': True}
        self.client = Mock(post=AsyncMock(return_value=response), aclose=AsyncMock())
        self.http.AsyncClient = Mock(return_value=self.client)
        spec = importlib.util.spec_from_file_location('alert_under_test', ROOT / 'legacy/backend/security_service.py')
        self.module = importlib.util.module_from_spec(spec)
        with patch.dict(sys.modules, {'cv2': self.cv, 'httpx': self.http}):
            spec.loader.exec_module(self.module)
        with patch.dict('os.environ', {'TELEGRAM_BOT_TOKEN': 'test-token', 'TELEGRAM_CHAT_ID': '123'}):
            self.service = self.module.SecurityService()

    def tearDown(self):
        run_immediate(self.service.close())

    def test_first_unknown_sends_current_frame_as_photo(self):
        frame = object()
        result = run_immediate(self.service.on_access_deny(frame))
        self.assertEqual(result, {'action': 'notification_sent', 'count': 1})
        self.cv.imencode.assert_called_once_with('.jpg', frame)
        args = self.client.post.call_args
        self.assertTrue(args.args[0].endswith('/sendPhoto'))
        self.assertEqual(args.kwargs['files']['photo'], ('alert.jpg', b'jpeg-data', 'image/jpeg'))
        self.assertEqual(args.kwargs['data']['chat_id'], '123')
        self.assertIn('caption', args.kwargs['data'])

    def test_disabled_notification_does_not_send(self):
        self.service.config['enable_notification'] = False
        self.assertEqual((run_immediate(self.service.on_access_deny(object())))['action'], 'notification_not_sent')
        self.client.post.assert_not_called()

    def test_missing_configuration_does_not_send(self):
        self.service.config['telegram_bot_token'] = ''
        run_immediate(self.service.on_access_deny(object()))
        self.client.post.assert_not_called()

    def test_bad_encoding_does_not_send(self):
        self.cv.imencode.return_value = (False, None)
        self.assertEqual((run_immediate(self.service.on_access_deny(object())))['action'], 'notification_not_sent')
        self.client.post.assert_not_called()

    def test_transport_failure_is_reported(self):
        self.client.post.side_effect = self.http.HTTPError()
        self.assertEqual((run_immediate(self.service.on_access_deny(object())))['action'], 'notification_not_sent')

    def test_telegram_rejection_is_not_success(self):
        self.client.post.return_value.json.return_value = {'ok': False}
        self.assertEqual((run_immediate(self.service.on_access_deny(object())))['action'], 'notification_not_sent')

    def test_known_face_resets_counter(self):
        run_immediate(self.service.on_access_deny(object()))
        self.service.on_access_granted()
        self.assertEqual(self.service.deny_count, 0)

if __name__ == '__main__':
    unittest.main()
