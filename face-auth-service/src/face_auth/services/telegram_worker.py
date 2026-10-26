"""Gửi hàng đợi PostgreSQL, không làm request đăng nhập chờ Telegram."""
import asyncio
import logging
import time
from src.face_auth.db.notification_store import NotificationQueue
from src.face_auth.db.storage import DatabaseUnavailable

logger = logging.getLogger(__name__)


class TelegramWorker:
    def __init__(self, store, notifier, clock=time.time):
        self.queue = NotificationQueue(store)
        self.notifier = notifier
        self.clock = clock

    def deliver_next(self):
        with self.queue.store.connect() as db:
            now = self.clock()
            self.queue.discard_expired(db, now)
            row = self.queue.next_message(db, now)
            if row is None:
                return False
            message_id, event, photo, admin_id, reason, created_at, attempts = row
            try:
                sent = self.notifier.send(event, str(message_id), bytes(photo), admin_id,
                                          reason=reason, occurred_at=created_at) is True
            except Exception as exc:
                # Không in exception vì HTTP URL Telegram có thể chứa bot token.
                logger.warning("Telegram adapter failed: %s", type(exc).__name__)
                sent = False
            self.queue.mark_result(db, message_id, sent, attempts, self.clock())
        return True

    async def run(self, stop):
        while not stop.is_set():
            try:
                processed = await asyncio.to_thread(self.deliver_next)
            except DatabaseUnavailable:
                logger.warning("Telegram queue database unavailable")
                processed = False
            except Exception as exc:
                logger.warning("Telegram worker failed: %s", type(exc).__name__)
                processed = False
            if processed:
                continue
            try:
                await asyncio.wait_for(stop.wait(), timeout=2)
            except asyncio.TimeoutError:
                pass
