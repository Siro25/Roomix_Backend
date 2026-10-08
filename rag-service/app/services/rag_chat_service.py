import json
import logging
from typing import List, Dict, Any
from app.models.schemas import ChatRequest, ChatResponse
from app.core.vector_store import vector_store
from app.core.gemini_client import gemini_client
from app.services.market_data_service import market_data_service

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """
Bạn là "Roomix AI" - Trợ lý thông minh độc quyền của nền tảng tìm kiếm và quản lý phòng trọ Roomix tại Việt Nam.
Nhiệm vụ của bạn là:
1. Hỗ trợ khách thuê (TENANT) tìm kiếm phòng trọ, căn hộ mini lý tưởng dựa trên ngữ cảnh dữ liệu được cung cấp (vị trí, ngân sách, diện tích, tiện nghi).
2. Giải thích rõ ràng các quy định cọc, tiền điện nước, nội quy, pháp lý hợp đồng thuê phòng theo luật pháp Việt Nam.
3. Hỗ trợ chủ trọ (LANDLORD) giải đáp cách đăng tin, quản lý khách thuê và giải quyết tranh chấp.

NGUYÊN TẮC QUAN TRỌNG:
- Luôn ưu tiên thông tin chính xác từ [NGỮ CẢNH DỮ LIỆU PHÒNG TRỌ VÀ CHÍNH SÁCH ĐƯỢC CUNG CẤP].
- Nếu có phòng phù hợp trong ngữ cảnh, hãy giới thiệu chi tiết (Tên phòng, Địa chỉ, Giá thuê, Tiện ích nổi bật, SĐT liên hệ của chủ trọ).
- Nếu dữ liệu trong hệ thống chưa có phòng hoàn toàn khớp 100%, hãy thành thật gợi ý các phòng gần giống nhất và đưa ra lời khuyên.
- Giọng văn lịch sự, nhiệt tình, chuyên nghiệp, tự nhiên bằng tiếng Việt.
- Sử dụng Markdown gọn gàng, định dạng giá tiền rõ ràng (ví dụ: 3.500.000 đ/tháng).
"""

class RagChatService:
    def process_chat(self, req: ChatRequest) -> ChatResponse:
        user_query = req.message
        filters = req.filters

        # 1. Search Room Knowledge Base
        city = filters.city if filters else None
        district = filters.district if filters else None
        min_p = filters.min_price if filters else None
        max_p = filters.max_price if filters else None
        room_type = filters.room_type if filters else None

        matched_rooms_raw = vector_store.search_rooms(
            query=user_query,
            n_results=4,
            city=city,
            district=district,
            min_price=min_p,
            max_price=max_p,
            room_type=room_type
        )

        # 2. Search Policy Knowledge Base
        matched_policies_raw = vector_store.search_policies(
            query=user_query,
            n_results=2
        )

        # 3. Build Context String
        rooms_context = ""
        market_context_parts = []
        matched_rooms_data = []
        for r in matched_rooms_raw:
            meta = r.get("metadata", {})
            try:
                amenities = json.loads(meta.get("amenities", "[]"))
            except Exception:
                amenities = []
            market_evaluation = market_data_service.evaluate_room(
                price=meta.get("price"),
                district=meta.get("district", ""),
                amenities=amenities,
                area_sqm=meta.get("area_sqm"),
                title=meta.get("title", ""),
            )
            if market_evaluation:
                market_context_parts.append(f"{meta.get('title', 'Phòng trọ')}:\n{market_evaluation}")
            matched_rooms_data.append({
                "id": r.get("id"),
                "title": meta.get("title"),
                "address": f"{meta.get('ward', '')}, {meta.get('district', '')}, {meta.get('city', '')}".strip(", "),
                "district": meta.get("district"),
                "city": meta.get("city"),
                "price": meta.get("price"),
                "area_sqm": meta.get("area_sqm"),
                "amenities": amenities,
                "contact_phone": meta.get("contact_phone"),
                "landlord_name": meta.get("landlord_name")
            })
            rooms_context += f"- [Mã: {r.get('id')}] {r.get('document')}\n\n"

        market_context = "\n\n".join(market_context_parts)

        policies_context = ""
        source_docs = []
        for p in matched_policies_raw:
            policies_context += f"{p.get('document')}\n\n"
            title = p.get("metadata", {}).get("title", "")
            if title:
                source_docs.append(title)

        # 4. Build Full Prompt with History
        history_str = ""
        if req.conversation_history:
            for msg in req.conversation_history[-6:]:
                role_label = "Khách hàng" if msg.role == "user" else "Roomix AI"
                history_str += f"{role_label}: {msg.content}\n"

        prompt = f"""
[LỊCH SỬ HỘI THOẠI TRƯỚC ĐÓ]:
{history_str if history_str else "Chưa có"}

[NGỮ CẢNH DỮ LIỆU PHÒNG TRỌ ROOMIX KHẢ DỤNG]:
{rooms_context if rooms_context else "Hiện không tìm thấy phòng trọ nào khớp trong bộ lọc."}

[ĐÁNH GIÁ GIÁ THỊ TRƯỜNG TỪ CSV]:
{market_context if market_context else "Chưa có dữ liệu CSV thị trường để đánh giá."}

[NGỮ CẢNH QUY ĐỊNH & PHÁP LÝ LIÊN QUAN]:
{policies_context if policies_context else "Không có điều khoản đặc thù."}

[VAI TRÒ NGƯỜI DÙNG]: {req.user_role}
[CÂU HỎI HIỆN TẠI]: {user_query}

Hãy trả lời câu hỏi của người dùng dựa trên các ngữ cảnh trên:
"""

        answer = gemini_client.generate_content(prompt, system_instruction=SYSTEM_PROMPT)

        # 5. Dynamic follow-ups
        follow_ups = self._generate_suggested_follow_ups(user_query, matched_rooms_data)

        return ChatResponse(
            answer=answer,
            matched_rooms=matched_rooms_data,
            source_documents=source_docs,
            suggested_follow_ups=follow_ups
        )

    def _generate_suggested_follow_ups(self, query: str, matched_rooms: List[Dict[str, Any]]) -> List[str]:
        q_lower = query.lower()
        if "cọc" in q_lower or "hợp đồng" in q_lower:
            return [
                "Nếu chuyển đi trước hạn thì có lấy lại được cọc không?",
                "Quy định đăng ký tạm trú cho người thuê trọ như thế nào?",
                "Hợp đồng thuê phòng trọ cần lưu ý những điều khoản nào?"
            ]
        elif matched_rooms:
            district = matched_rooms[0].get("district", "khu vực này")
            return [
                f"Có phòng nào có gác lửng và ban công tại {district} không?",
                "Chi phí điện nước và phụ phí của các phòng trên thế nào?",
                "Làm sao để đặt lịch hẹn xem phòng với chủ trọ?"
            ]
        else:
            return [
                "Tìm phòng trọ giá dưới 4 triệu tại Quận 7",
                "Căn hộ mini Studio gần Bình Thạnh có ban công",
                "Cách thức cọc giữ chỗ an toàn qua Roomix"
            ]


rag_chat_service = RagChatService()
