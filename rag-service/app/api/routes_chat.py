import logging
from fastapi import APIRouter, HTTPException
from app.models.schemas import ChatRequest, ChatResponse
from app.services.rag_chat_service import rag_chat_service

router = APIRouter(prefix="/chat", tags=["RAG Chat & Support"])
logger = logging.getLogger(__name__)

@router.post("", response_model=ChatResponse, summary="Chat tư vấn tìm phòng trọ và giải đáp quy chế")
def chat_with_rag(request: ChatRequest):
    """
    Điểm cuối tư vấn khách hàng thuê trọ & chủ trọ bằng RAG:
    - Tìm kiếm phòng trọ thông minh theo ngữ cảnh (vị trí, khoảng giá, tiện ích)
    - Giải đáp chính sách tiền cọc, điện nước, hợp đồng pháp lý
    """
    try:
        response = rag_chat_service.process_chat(request)
        return response
    except Exception as e:
        logger.error(f"Error in chat endpoint: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Lỗi xử lý tư vấn RAG: {str(e)}")
