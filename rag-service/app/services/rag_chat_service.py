import json
import logging
from typing import List, Dict, Any
from app.models.schemas import ChatRequest, ChatResponse
from app.core.vector_store import vector_store
from app.core.gemini_client import gemini_client
from app.services.pricing_service import pricing_service
from app.services.query_parser import parse_query

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """
Bạn là "Roomix AI" - Trợ lý thông minh độc quyền của nền tảng tìm kiếm và quản lý phòng trọ Roomix tại Việt Nam.
Nhiệm vụ của bạn là:
1. Hỗ trợ khách thuê (TENANT) tìm kiếm phòng trọ, căn hộ mini lý tưởng dựa trên ngữ cảnh dữ liệu được cung cấp (vị trí, ngân sách, diện tích, tiện nghi).
2. Giải thích rõ ràng các quy định cọc, tiền điện nước, nội quy, pháp lý hợp đồng thuê phòng theo luật pháp Việt Nam.
3. Hỗ trợ chủ trọ (LANDLORD) giải đáp cách đăng tin, quản lý khách thuê và giải quyết tranh chấp.

NGUYÊN TẮC QUAN TRỌNG:
- Luôn ưu tiên thông tin chính xác từ [NGỮ CẢNH DỮ LIỆU PHÒNG TRỌ VÀ CHÍNH SÁCH ĐƯỢC CUNG CẤP].
- Danh sách phòng trong ngữ cảnh là dữ liệu THẬT đang cho thuê trên hệ thống. CHỈ được nói về các phòng có trong ngữ cảnh; TUYỆT ĐỐI không bịa tên phòng, giá, địa chỉ, tiện ích, số điện thoại hay đường dẫn.
- Nếu có phòng phù hợp trong ngữ cảnh, hãy giới thiệu chi tiết (Tên phòng, Địa chỉ, Giá thuê, Tiền cọc nếu có, Diện tích, Tiện ích nổi bật, Tên chủ trọ & SĐT liên hệ nếu có trong ngữ cảnh, thời gian có thể dọn vào nếu có) và nhắc kèm mã phòng.
- Nếu khách hỏi thông tin liên hệ mà một thông tin không có trong ngữ cảnh (ví dụ phòng chưa cập nhật SĐT), hãy nói rõ là chưa có thông tin và hướng dẫn khách xem chi tiết phòng trên Roomix rồi gửi yêu cầu thuê / đặt lịch xem phòng.
- Đánh giá mức giá (Đắt / Rẻ / Hợp lý): Mỗi phòng trong ngữ cảnh đều có thông tin [Đánh giá giá hệ thống]. Khi khách hỏi về giá (ví dụ 'phòng này giá thế nào, có đắt không?', 'giá này có cao không?') hoặc khi tư vấn, hãy dựa vào đánh giá này để nhận xét khách quan (giá rẻ/hợp lý/cao dựa trên diện tích, vị trí và tiện ích).
- Nếu ngữ cảnh ghi chú không có phòng khớp đúng tiêu chí, hãy nói thẳng điều đó trước, rồi mới gợi ý các phòng gần nhất kèm lý do chưa khớp (ví dụ giá cao hơn ngân sách bao nhiêu).
- Nếu hệ thống chưa có phòng nào trong ngữ cảnh, hãy nói chưa tìm thấy phòng phù hợp và gợi ý khách thử nới tiêu chí; không tự tạo phòng.
- Giọng văn lịch sự, nhiệt tình, chuyên nghiệp, tự nhiên bằng tiếng Việt.
- Sử dụng Markdown gọn gàng, định dạng giá tiền rõ ràng (ví dụ: 3.500.000 đ/tháng).
"""

class RagChatService:
    def process_chat(self, req: ChatRequest) -> ChatResponse:
        user_query = req.message
        filters = req.filters

        # 1. Work out the constraints: explicit request filters win, otherwise
        # fall back to what the user wrote in natural language.
        parsed = parse_query(user_query, vector_store.known_locations())

        def pick(explicit, inferred):
            return explicit if explicit is not None else inferred

        full = dict(
            city=pick(filters.city if filters else None, parsed.city),
            district=pick(filters.district if filters else None, parsed.district),
            ward=parsed.ward,
            min_price=pick(filters.min_price if filters else None, parsed.min_price),
            max_price=pick(filters.max_price if filters else None, parsed.max_price),
            room_type=filters.room_type if filters else None,
            min_area=pick(filters.min_area if filters else None, parsed.min_area),
            max_area=pick(filters.max_area if filters else None, parsed.max_area),
        )
        # Progressive relaxation: exact -> location only -> everything.
        location_only = {k: full[k] for k in ("city", "district", "ward")}
        has_other_filters = any(v is not None for k, v in full.items() if k not in location_only)
        attempts = [full]
        if has_other_filters and any(location_only.values()):
            attempts.append(location_only)
        if any(v is not None for v in full.values()):
            attempts.append({})

        matched_rooms_raw = []
        relaxed_level = 0
        for level, attempt in enumerate(attempts):
            matched_rooms_raw = vector_store.search_rooms(
                query=user_query, n_results=4, **attempt
            )
            relaxed_level = level
            if matched_rooms_raw:
                break
        relaxed = relaxed_level > 0 and bool(matched_rooms_raw)
        criteria_text = ", ".join(f"{k}={v}" for k, v in full.items() if v is not None)

        # 2. Search Policy Knowledge Base
        matched_policies_raw = vector_store.search_policies(
            query=user_query,
            n_results=2
        )

        # 3. Build Context String
        rooms_context = ""
        matched_rooms_data = []
        for r in matched_rooms_raw:
            meta = r.get("metadata", {})
            try:
                amenities = json.loads(meta.get("amenities", "[]"))
            except Exception:
                amenities = []

            price_val = meta.get("price")
            area_val = meta.get("area_sqm")
            district_val = meta.get("district", "")
            price_assessment = pricing_service.evaluate_room_price(
                price=float(price_val) if price_val else 0.0,
                area_sqm=float(area_val) if area_val else 0.0,
                district=district_val,
                amenities=amenities,
                room_type=meta.get("room_type", "PHONG_TRO"),
            )

            matched_rooms_data.append({
                "id": r.get("id"),
                "title": meta.get("title"),
                "address": ", ".join(p for p in (meta.get("address"), meta.get("ward"), meta.get("district"), meta.get("city")) if p),
                "district": meta.get("district"),
                "city": meta.get("city"),
                "price": meta.get("price"),
                "deposit": meta.get("deposit") or None,
                "area_sqm": meta.get("area_sqm"),
                "max_tenants": meta.get("max_tenants") or None,
                "image_url": meta.get("image_url") or None,
                "amenities": amenities,
                "contact_phone": meta.get("contact_phone"),
                "landlord_name": meta.get("landlord_name"),
                "price_assessment": price_assessment,
            })
            eval_tag = f" - [Đánh giá giá hệ thống: {price_assessment['label']} - {price_assessment['explanation']}]"
            rooms_context += f"- [Mã: {r.get('id')}] {r.get('document')}{eval_tag}\n\n"

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

        if relaxed:
            rooms_note = (
                f"[LƯU Ý HỆ THỐNG: KHÔNG có phòng nào khớp đúng tiêu chí ({criteria_text}). "
                "Các phòng dưới đây chỉ là gần nhất, hãy nói rõ điều này và chỉ rõ phần chưa khớp.]\n"
            )
        elif criteria_text and matched_rooms_raw:
            rooms_note = f"[Tiêu chí áp dụng: {criteria_text}. Tất cả phòng dưới đây đều khớp tiêu chí.]\n"
        else:
            rooms_note = ""
        no_rooms_text = (
            "Hệ thống hiện không có phòng nào đang cho thuê (hoặc chưa đồng bộ được dữ liệu phòng)."
        )

        prompt = f"""
[LỊCH SỬ HỘI THOẠI TRƯỚC ĐÓ]:
{history_str if history_str else "Chưa có"}

[NGỮ CẢNH DỮ LIỆU PHÒNG TRỌ ROOMIX KHẢ DỤNG]:
{rooms_note}{rooms_context if rooms_context else no_rooms_text}

[NGỮ CẢNH QUY ĐỊNH & PHÁP LÝ LIÊN QUAN]:
{policies_context if policies_context else "Không có điều khoản đặc thù."}

[VAI TRÒ NGƯỜI DÙNG]: {req.user_role}
[CÂU HỎI HIỆN TẠI]: {user_query}

Hãy trả lời câu hỏi của người dùng dựa trên các ngữ cảnh trên:
"""

        answer = gemini_client.generate_content(prompt, system_instruction=SYSTEM_PROMPT) or \
            "Xin lỗi, hiện tôi chưa thể đưa ra câu trả lời. Vui lòng thử lại."

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
