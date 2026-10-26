import logging
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

import httpx

from app.config import settings
from app.core.vector_store import vector_store
from app.models.schemas import RoomItem

logger = logging.getLogger(__name__)

_DETAIL_WORKERS = 8


class CoreSyncError(RuntimeError):
    pass


def _num(value: Any) -> Optional[float]:
    return float(value) if value is not None else None


def _amenities(data: Dict[str, Any]) -> List[str]:
    items: List[str] = []
    if data.get("hasPrivateBathroom"):
        items.append("Khép kín (WC riêng)")
    if data.get("hasAirConditioner"):
        items.append("Máy lạnh")
    if data.get("hasWaterHeater"):
        items.append("Bình nóng lạnh")
    if data.get("hasBalcony"):
        items.append("Ban công")
    extra = (data.get("amenitiesDescription") or "").replace(";", ",").replace("\n", ",")
    items.extend(part.strip() for part in extra.split(",") if part.strip())
    return items


def room_from_core(summary: Dict[str, Any], detail: Optional[Dict[str, Any]]) -> RoomItem:
    """Map core-service room payloads onto the RAG RoomItem.

    `detail` (RoomDetailResponse) carries description/deposit/images; `summary`
    (RoomSummaryResponse) is used as the base so a room is still indexable if
    the detail call is unavailable.
    """
    merged = {**summary, **(detail or {})}
    images = merged.get("imageUrls") or []
    return RoomItem(
        id=str(merged["roomId"]),
        title=merged.get("title") or f"Phòng {merged.get('roomNumber', '')}".strip(),
        description=merged.get("description") or "",
        address=merged.get("addressStreet") or "",
        ward=merged.get("ward") or "",
        district=merged.get("district") or "",
        city=merged.get("city") or "",
        area_sqm=_num(merged.get("area")) or 0.0,
        price=_num(merged.get("rentalPrice")) or 0.0,
        deposit=_num(merged.get("depositAmount")),
        amenities=_amenities(merged),
        available=bool(merged.get("available", True)) and not merged.get("rented", False),
        max_tenants=merged.get("maxTenants"),
        available_from=str(merged["availableFrom"]) if merged.get("availableFrom") else None,
        house_name=merged.get("houseName") or "",
        room_number=merged.get("roomNumber") or "",
        image_url=images[0] if images else (merged.get("primaryImageUrl") or ""),
        contact_phone=merged.get("contactPhone") or merged.get("contact_phone") or merged.get("phoneNumber") or "",
        landlord_name=merged.get("landlordName") or merged.get("landlord_name") or "",
    )


class CoreRoomSync:
    """Mirrors the public rooms of core-service into the vector store."""

    def __init__(self):
        self._lock = threading.Lock()
        self.last_status: Dict[str, Any] = {"state": "never_run"}

    @property
    def enabled(self) -> bool:
        return bool(settings.CORE_SERVICE_URL)

    # ------------------------------------------------------------------
    def _get(self, client: httpx.Client, path: str, params: Optional[dict] = None) -> Any:
        response = client.get(path, params=params)
        response.raise_for_status()
        body = response.json()
        return body.get("data") if isinstance(body, dict) and "data" in body else body

    def fetch_rooms(self) -> List[RoomItem]:
        base_url = settings.CORE_SERVICE_URL.rstrip("/")
        timeout = settings.CORE_SYNC_TIMEOUT_SECONDS
        summaries: List[Dict[str, Any]] = []
        with httpx.Client(base_url=base_url, timeout=timeout) as client:
            page = 0
            while True:
                data = self._get(client, "/api/rooms/search", {
                    "page": page,
                    "size": settings.CORE_SYNC_PAGE_SIZE,
                    "sort": "newest",
                })
                summaries.extend(data.get("content", []))
                if data.get("last", True):
                    break
                page += 1

            def load(summary: Dict[str, Any]) -> Optional[RoomItem]:
                try:
                    detail = self._get(client, f"/api/rooms/{summary['roomId']}")
                except httpx.HTTPStatusError as e:
                    if e.response.status_code in (400, 404, 409):
                        return None  # room was taken off the market meanwhile
                    raise
                return room_from_core(summary, detail)

            with ThreadPoolExecutor(max_workers=_DETAIL_WORKERS) as pool:
                rooms = [r for r in pool.map(load, summaries) if r is not None]
        return [r for r in rooms if r.available]

    def sync(self) -> Dict[str, Any]:
        """Fetch rooms from core and reconcile the index. Never deletes on failure."""
        if not self.enabled:
            raise CoreSyncError("CORE_SERVICE_URL is not configured.")
        if not self._lock.acquire(blocking=False):
            return {"state": "already_running"}
        started = time.monotonic()
        try:
            try:
                rooms = self.fetch_rooms()
            except Exception as e:
                logger.error(f"Room sync failed while fetching from core-service: {e}")
                self.last_status = {
                    "state": "error",
                    "error": str(e),
                    "at": datetime.now(timezone.utc).isoformat(),
                }
                raise CoreSyncError(str(e)) from e
            stats = vector_store.sync_rooms(rooms)
            self.last_status = {
                "state": "ok",
                "at": datetime.now(timezone.utc).isoformat(),
                "duration_seconds": round(time.monotonic() - started, 2),
                **stats,
            }
            logger.info(f"Room sync finished: {self.last_status}")
            return self.last_status
        finally:
            self._lock.release()


core_room_sync = CoreRoomSync()
