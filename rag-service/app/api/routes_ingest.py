import json
import os
import logging
from fastapi import APIRouter, HTTPException
from app.models.schemas import IngestRoomsRequest, IngestResponse, RoomItem
from app.core.vector_store import vector_store

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
        raise HTTPException(status_code=500, detail=f"Lỗi nạp dữ liệu phòng: {str(e)}")


@router.post("/init-sample-data", response_model=IngestResponse, summary="Khởi tạo dữ liệu mẫu ban đầu (phòng trọ & chính sách)")
def init_sample_data():
    """
    Nạp dữ liệu mẫu ban đầu từ thư mục data để kiểm thử hệ thống ngay lập tức.
    """
    try:
        base_dir = os.path.dirname(os.path.dirname(__file__))
        rooms_file = os.path.join(base_dir, "data", "sample_rooms.json")
        rules_file = os.path.join(base_dir, "data", "rental_rules.json")

        room_count = 0
        if os.path.exists(rooms_file):
            with open(rooms_file, "r", encoding="utf-8") as f:
                raw_rooms = json.load(f)
                rooms = [RoomItem(**r) for r in raw_rooms]
                room_count = vector_store.add_rooms(rooms)

        rule_count = 0
        if os.path.exists(rules_file):
            with open(rules_file, "r", encoding="utf-8") as f:
                rules = json.load(f)
                rule_count = vector_store.add_policy_documents(rules)

        return IngestResponse(
            success=True,
            indexed_count=room_count + rule_count,
            message=f"Khởi tạo thành công: {room_count} phòng trọ và {rule_count} điều khoản chính sách vào Vector Store."
        )
    except Exception as e:
        logger.error(f"Error loading sample data: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Lỗi khởi tạo dữ liệu mẫu: {str(e)}")
