import json
import logging
import re
from typing import List, Dict, Any
from app.models.schemas import ChatRequest, ChatResponse
from app.core.vector_store import vector_store
from app.core.gemini_client import gemini_client
from app.services.pricing_service import pricing_service
from app.services.query_parser import parse_query

logger = logging.getLogger(__name__)


def sanitize_chat_answer(text: str) -> str:
    """Loại bỏ ký tự đặc biệt, ký tự định dạng markdown như **, *, ~, ###, ` để văn bản sạch sẽ."""
    if not text:
        return ""
    # Thay thế ~ thành "khoảng "
    text = re.sub(r'~\s*', 'khoảng ', text)
    # Loại bỏ markdown in đậm **text** -> text
    text = re.sub(r'\*\*(.*?)\*\*', r'\1', text)
    # Loại bỏ markdown in nghiêng *text* -> text hoặc _text_ -> text
    text = re.sub(r'(?<!\w)\*(.*?)\*(?!\w)', r'\1', text)
    text = re.sub(r'(?<!\w)_(.*?)_(?!\w)', r'\1', text)
    # Loại bỏ tiêu đề markdown ###, ##, #
    text = re.sub(r'^\s*#+\s*', '', text, flags=re.MULTILINE)
    # Loại bỏ dấu backtick `
    text = text.replace('`', '')
    # Loại bỏ dấu sao còn sót lại
    text = text.replace('*', '')
    # Chuẩn hóa khoảng trắng thừa
    text = re.sub(r'[ \t]+', ' ', text)
    return text.strip()


SYSTEM_PROMPT = """
Bạn là "Roomix AI" - Trợ lý thông minh độc quyền của nền tảng tìm kiếm và quản lý phòng trọ Roomix tại Việt Nam.

PHONG CÁCH VÀ NGUYÊN TẮC TRẢ LỜI (BẮT BUỘC TUÂN THỦ):
1. NGẮN GỌN & ĐI THẲNG VÀO TRỌNG TÂM:
- Tuyệt đối KHÔNG chào hỏi rườm rà (không "Chào bạn, tôi là Roomix AI...", không "Rất vui được hỗ trợ..."). Đi thẳng vào giải đáp câu hỏi của người dùng.
- KHÔNG liệt kê hay sao chép lại thông số chi tiết của phòng (địa chỉ đầy đủ, diện tích, tiền cọc, số điện thoại, quy định, v.v.) vì hệ thống giao diện Roomix ĐÃ TỰ ĐỘNG HIỂN THỊ CÁC THẺ PHÒNG TRỌ (Room Cards) trực quan ngay bên dưới tin nhắn của bạn.
- Trả lời cô đọng trong 1-2 đoạn văn ngắn hoặc các gạch đầu dòng súc tích, chỉ tập trung giải quyết đúng mục tiêu người dùng tìm kiếm.

2. ĐÁNH GIÁ SẮC BÉN, CỤ THỂ, CÓ DỮ LIỆU (TUYỆT ĐỐI KHÔNG NÓI CHUNG CHUNG):
- Tuyệt đối tránh các nhận xét sáo rỗng, vu vơ như "phòng này giá tốt", "tiện nghi đầy đủ", "rất đáng sống".
- Với mỗi phòng gợi ý, chỉ cần đánh giá nhanh 2 ý cốt lõi:
  + Ưu điểm đắt giá nhất giải quyết đúng nhu cầu (ví dụ: "gần Bến xe Mỹ Đình đi lại thuận tiện", "studio Thanh Xuân có thang máy và bảo vệ 24/7", "cho phép nuôi thú cưng").
  + Đánh giá giá trị thực tế dựa trên [Đánh giá giá hệ thống]: Nêu rõ lý do và số liệu cụ thể (ví dụ: "Giá 2.300.000 đ/tháng rẻ hơn khoảng 15% so với mức định giá 2.700.000 đ của khu vực Nam Từ Liêm", hoặc "Giá 5.200.000 đ thuộc phân khúc cao cấp nhưng hợp lý nhờ full nội thất & thang máy").
  + Lưu ý thực tế (nếu có: ví dụ tiền cọc 1 tháng, điện 3.500đ/kWh).

3. ĐỊNH DẠNG VĂN BẢN SẠCH SẼ - TUYỆT ĐỐI KHÔNG DÙNG KÝ TỰ ĐẶC BIỆT:
- Giao diện người dùng hiển thị văn bản thuần túy, KHÔNG hỗ trợ ký tự Markdown.
- TUYỆT ĐỐI KHÔNG DÙNG dấu sao đôi (**), không dùng dấu sao đơn (*), không dùng dấu ngã (~), không dùng dấu thăng (###), không dùng dấu gạch dưới (_), không dùng dấu nháy ngược (`).
- Khi muốn viết xấp xỉ, hãy viết từ "khoảng " thay vì dấu "~" (ví dụ: "rẻ hơn khoảng 20%").
- Khi muốn làm nổi bật tiêu đề phòng, hãy viết hoa hoặc dùng dấu hai chấm thông thường (ví dụ: "Phòng 20m² khép kín - 2.300.000 đ/tháng: Ưu điểm...").
- Dùng dấu gạch đầu dòng (-) đơn giản ở đầu dòng để liệt kê.

4. ĐỘ CHÍNH XÁC & TRUNG THỰC VỀ DỮ LIỆU:
- CHỈ nói về các phòng có trong [NGỮ CẢNH DỮ LIỆU PHÒNG TRỌ ROOMIX KHẢ DỤNG]. Tuyệt đối không tự bịa thông tin phòng hay liên hệ ngoài hệ thống.
- Nếu không có phòng khớp đúng 100% tiêu chí, nói thẳng ngay câu đầu tiên rằng chưa có phòng khớp hoàn toàn và chỉ rõ các phòng gợi ý bị lệch ở điểm nào (ví dụ: giá cao hơn ngân sách bao nhiêu).
- Nếu không có phòng nào trong hệ thống, thông báo ngắn gọn và gợi ý mở rộng tiêu chí.
- Định dạng tiền tệ rõ ràng bằng tiếng Việt (ví dụ: 2.300.000 đ/tháng).
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

CHỈ DẪN TRẢ LỜI:
- Trả lời thật ngắn gọn, súc tích, đi thẳng vào trọng tâm câu hỏi.
- Không liệt kê lặp lại toàn bộ thông số chi tiết của phòng vì giao diện đã hiển thị các thẻ phòng trực quan bên dưới.
- Đánh giá cụ thể từng lựa chọn: nêu bật 1 ưu điểm đắt giá nhất và phân tích giá sắc bén dựa trên [Đánh giá giá hệ thống] (rẻ hơn / hợp lý / cao hơn bao nhiêu % so với mặt bằng thị trường khu vực).
- TUYỆT ĐỐI KHÔNG dùng ký tự đặc biệt như **, *, ~, ###, ` để văn bản hiển thị sạch sẽ, tự nhiên.
"""

        raw_answer = gemini_client.generate_content(prompt, system_instruction=SYSTEM_PROMPT) or \
            "Xin lỗi, hiện tôi chưa thể đưa ra câu trả lời. Vui lòng thử lại."
        answer = sanitize_chat_answer(raw_answer)

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
