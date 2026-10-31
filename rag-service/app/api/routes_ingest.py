import json
import os
import logging
from typing import List
from fastapi import APIRouter, HTTPException, Query
from app.models.schemas import IngestRoomsRequest, IngestResponse, RoomItem
from app.core.vector_store import vector_store
from app.services.core_room_sync import core_room_sync, CoreSyncError

router = APIRouter(prefix="/ingest", tags=["Data Ingestion & Indexing"])
logger = logging.getLogger(__name__)

@router.post("/rooms", response_model=IngestResponse, summary="Đồng bộ danh sách phòng trọ vào Vector Database")
def ingest_rooms(request: IngestRoomsRequest):
    """
    Nhận danh sách phòng trọ (từ rental-service hoặc admin) và đưa vào ChromaDB để phục vụ tìm kiếm ngữ nghĩa.
    """
    try:
        count = vector_store.add_rooms(request.rooms)
        return IngestResponse(
            success=True,
            indexed_count=count,
            message=f"Đã lập chỉ mục thành công {count} phòng trọ vào cơ sở dữ liệu vector."
        )
    except Exception as e:
        logger.error(f"Error ingesting rooms: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Lỗi nạp dữ liệu phòng.")


@router.delete("/rooms", summary="Xóa phòng trọ khỏi Vector Database")
def delete_rooms(ids: List[str] = Query(..., description="Danh sách id phòng cần xóa")):
    """
    Gỡ các phòng đã bị xóa/ẩn ở hệ thống chính khỏi chỉ mục tìm kiếm.
    """
    try:
        count = vector_store.delete_rooms(ids)
        return {"success": True, "deleted_count": count}
    except Exception as e:
        logger.error(f"Error deleting rooms: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Lỗi xóa dữ liệu phòng.")


def _data_path(name: str) -> str:
    return os.path.join(os.path.dirname(os.path.dirname(__file__)), "data", name)


def seed_policies() -> int:
    """Load the rental rules/policies knowledge base (static legal content)."""
    rules_file = _data_path("rental_rules.json")
    if not os.path.exists(rules_file):
        return 0
    with open(rules_file, "r", encoding="utf-8") as f:
        return vector_store.add_policy_documents(json.load(f))


def seed_sample_rooms() -> int:
    """Load DEMO rooms. Never use alongside real data from core-service."""
    rooms_file = _data_path("sample_rooms.json")
    if not os.path.exists(rooms_file):
        return 0
    with open(rooms_file, "r", encoding="utf-8") as f:
        return vector_store.add_rooms([RoomItem(**r) for r in json.load(f)])


@router.post("/init-sample-data", response_model=IngestResponse, summary="Khởi tạo dữ liệu mẫu ban đầu (chính sách & phòng demo)")
def init_sample_data():
    """
    Nạp chính sách mẫu. Phòng demo chỉ được nạp khi KHÔNG cấu hình CORE_SERVICE_URL
    (khi có core-service, dữ liệu phòng luôn lấy từ hệ thống thật).
    """
    try:
        rule_count = seed_policies()
        room_count = 0 if core_room_sync.enabled else seed_sample_rooms()
        return IngestResponse(
            success=True,
            indexed_count=room_count + rule_count,
            message=f"Khởi tạo thành công: {room_count} phòng trọ và {rule_count} điều khoản chính sách vào Vector Store."
        )
    except Exception as e:
        logger.error(f"Error loading sample data: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Lỗi khởi tạo dữ liệu mẫu.")


@router.post("/sync", summary="Đồng bộ ngay phòng trọ từ core-service")
def sync_rooms_from_core():
    """Kéo toàn bộ phòng đang cho thuê từ core-service và làm mới chỉ mục tìm kiếm."""
    if not core_room_sync.enabled:
        raise HTTPException(status_code=409, detail="CORE_SERVICE_URL chưa được cấu hình.")
    try:
        return core_room_sync.sync()
    except CoreSyncError:
        raise HTTPException(status_code=502, detail="Không thể đồng bộ dữ liệu từ core-service.")


@router.get("/sync/status", summary="Trạng thái đồng bộ phòng gần nhất")
def sync_status():
    return {"enabled": core_room_sync.enabled, **core_room_sync.last_status}
