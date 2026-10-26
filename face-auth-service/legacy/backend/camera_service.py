"""Webcam source independent of hardware controllers."""
import asyncio
import cv2

class CameraService:
    def __init__(self, camera_index=0):
        self.cap = cv2.VideoCapture(camera_index)
        self._lock = asyncio.Lock()

    @property
    def is_connected(self):
        return self.cap.isOpened()

    async def capture_frame(self):
        async with self._lock:
            ok, frame = await asyncio.to_thread(self.cap.read)
        return cv2.flip(frame, 1) if ok else None

    async def close(self):
        async with self._lock:
            await asyncio.to_thread(self.cap.release)
