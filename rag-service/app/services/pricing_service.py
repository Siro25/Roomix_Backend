import json
import logging
from typing import Dict, Any, List, Optional
from app.models.schemas import PricingPredictRequest, PricingPredictResponse, PricingFactorBreakdown
from app.core.vector_store import vector_store
from app.core.gemini_client import gemini_client

logger = logging.getLogger(__name__)

# Base price per sqm benchmarks in VND for key districts (TP.HCM & Hanoi)
DISTRICT_BASE_RATES: Dict[str, float] = {
    # TP Hồ Chí Minh
    "quận 1": 220000,
    "quận 3": 190000,
    "quận 7": 150000,
    "bình thạnh": 160000,
    "phú nhuận": 170000,
    "tân bình": 140000,
    "gò vấp": 120000,
    "thành phố thủ đức": 125000,
    "quận 10": 165000,
    "quận 5": 145000,
    "quận 8": 110000,
    "bình tân": 95000,
    "quận 12": 90000,

    # Hà Nội
    "cầu giấy": 160000,
    "đống đa": 165000,
    "ba đình": 180000,
    "hai bà trưng": 160000,
    "thanh xuân": 140000,
    "nam từ liêm": 125000,
    "bắc từ liêm": 110000,
    "hà đông": 100000,
}

AMENITY_VALUES: Dict[str, float] = {
    "máy lạnh": 400000,
    "điều hòa": 400000,
    "tủ lạnh": 250000,
    "máy giặt": 200000,
    "bình nóng lạnh": 200000,
    "nóng lạnh": 200000,
    "thang máy": 200000,
    "ban công": 350000,
    "cửa sổ lớn": 200000,
    "kệ bếp": 200000,
    "bếp riêng": 350000,
    "giường nệm": 200000,
    "tủ quần áo": 150000,
    "khóa vân tay": 150000,
    "bảo vệ 24/7": 150000,
}

ROOM_TYPE_MULTIPLIERS: Dict[str, float] = {
    "PHONG_TRO": 1.0,
    "CHUNG_CU_MINI": 1.25,
    "CAN_HO_DICH_VU": 1.45,
    "NHA_NGUYEN_CAN": 1.15,
    "O_GHEP": 0.6
}


class PricingService:
    def predict_rental_price(self, req: PricingPredictRequest) -> PricingPredictResponse:
        district_key = req.district.strip().lower()
        base_rate = DISTRICT_BASE_RATES.get(district_key, 130000.0)

        multiplier = ROOM_TYPE_MULTIPLIERS.get(req.room_type, 1.0)
        adjusted_base_rate = base_rate * multiplier
        area_cost = adjusted_base_rate * req.area_sqm

        amenities_val = 0.0
        normalized_amenities = [a.strip().lower() for a in req.amenities]
        for a_key, val in AMENITY_VALUES.items():
            if any(a_key in item for item in normalized_amenities):
                amenities_val += val

        feature_bonus = 0.0
        if req.is_mezzanine:
            feature_bonus += 350000
        if req.is_studio:
            feature_bonus += 500000
        if req.is_newly_built:
            feature_bonus += area_cost * 0.12
        if req.has_parking:
            feature_bonus += 100000

        raw_estimated = area_cost + amenities_val + feature_bonus
        # Round to nearest 50,000 VND
        recommended = round(raw_estimated / 50000.0) * 50000.0
        min_price = round((recommended * 0.90) / 50000.0) * 50000.0
        max_price = round((recommended * 1.15) / 50000.0) * 50000.0

        # Retrieve comparable rooms from ChromaDB
        search_query = f"{req.room_type} tại {req.district} {req.city} diện tích {req.area_sqm} m2 {' '.join(req.amenities)}"
        matched = vector_store.search_rooms(
            query=search_query,
            n_results=4,
            district=req.district
        )

        comparables = []
        for m in matched:
            meta = m.get("metadata", {})
            try:
                amenities_list = json.loads(meta.get("amenities", "[]"))
            except Exception:
                amenities_list = []
            comparables.append({
                "id": m.get("id"),
                "title": meta.get("title"),
                "district": meta.get("district"),
                "price": meta.get("price"),
                "area_sqm": meta.get("area_sqm"),
                "amenities": amenities_list
            })

        # Market Analysis via Gemini based on system benchmarks & comparable rooms
        market_analysis = self._generate_ai_market_analysis(
            req, recommended, min_price, max_price, comparables
        )

        breakdown = PricingFactorBreakdown(
            base_price_per_sqm=base_rate,
            district_weight=multiplier,
            area_cost=area_cost,
            amenities_surcharge=amenities_val,
            feature_bonus=feature_bonus
        )

        return PricingPredictResponse(
            recommended_price=recommended,
            estimated_price_min=min_price,
            estimated_price_max=max_price,
            confidence_score=0.88,
            breakdown=breakdown,
            market_analysis=market_analysis,
            comparable_rooms=comparables
        )

    def _generate_ai_market_analysis(
        self,
        req: PricingPredictRequest,
        recommended: float,
        min_p: float,
        max_p: float,
        comparables: List[Dict[str, Any]],
    ) -> str:
        prompt = f"""
Bạn là chuyên gia thẩm định giá và tư vấn tối ưu vận hành bất động sản cho thuê (phòng trọ, căn hộ mini) tại Việt Nam của nền tảng Roomix.
Hãy phân tích cơ hội định giá và chiến lược cho thuê căn phòng sau cho chủ trọ dựa trên dữ liệu hệ thống:

THÔNG TIN PHÒNG CỦA CHỦ TRỌ:
- Khu vực: {req.district}, {req.city} (Phường: {req.ward or 'Trung tâm'})
- Loại hình: {req.room_type}
- Diện tích: {req.area_sqm} m2
- Tiện ích hiện có: {', '.join(req.amenities) if req.amenities else 'Cơ bản'}
- Gác lửng: {'Có' if req.is_mezzanine else 'Không'}
- Dạng Studio: {'Có' if req.is_studio else 'Không'}
- Mới xây/cải tạo: {'Mới xây' if req.is_newly_built else 'Cũ'}

KẾT QUẢ TÍNH TOÁN DỰ ĐOÁN TỪ HỆ THỐNG:
- Giá đề xuất tối ưu: {recommended:,.0f} VND/tháng
- Khung giá hệ thống: {min_p:,.0f} - {max_p:,.0f} VND/tháng
- Các phòng tương đương đang cho thuê trên hệ thống Roomix: {json.dumps(comparables, ensure_ascii=False) if comparables else 'Chưa có phòng tương đương cùng khu vực trên hệ thống'}

YÊU CẦU ĐẦU RA:
1. Đánh giá tính cạnh tranh của mức giá {recommended:,.0f} VND so với các phòng trong cùng khu vực {req.district}.
2. Phân khúc khách hàng mục tiêu phù hợp nhất (Sinh viên, Dân văn phòng, Gia đình trẻ,...).
3. Dự báo tỷ lệ lấp đầy và thời gian trung bình tìm được khách thuê.
4. Gợi ý 2-3 nâng cấp chi phí thấp giúp chủ trọ có thể nâng giá thêm 300k - 500k/tháng.
Viết ngắn gọn, chuyên nghiệp, dùng định dạng Markdown với bullet points rõ ràng.
"""
        analysis = gemini_client.generate_content(prompt)
        return analysis

    def evaluate_room_price(
        self,
        price: float,
        area_sqm: float,
        district: str,
        amenities: Optional[List[str]] = None,
        room_type: str = "PHONG_TRO",
    ) -> Dict[str, Any]:
        """Đánh giá mức giá của một phòng (GIÁ TỐT, GIÁ HỢP LÝ, GIÁ CAO)
        dựa trên định mức diện tích, tiện nghi và mặt bằng hệ thống Roomix.
        """
        if not price or price <= 0:
            return {
                "label": "CHƯA RÕ",
                "diff_percent": 0.0,
                "estimated_price": 0.0,
                "explanation": "Chưa có đủ thông tin giá thuê để đánh giá.",
            }

        area = float(area_sqm) if area_sqm and area_sqm > 0 else 20.0
        district_key = (district or "").strip().lower()
        base_rate = DISTRICT_BASE_RATES.get(district_key, 130000.0)

        multiplier = ROOM_TYPE_MULTIPLIERS.get((room_type or "PHONG_TRO").upper(), 1.0)
        area_cost = base_rate * multiplier * area

        amenities_val = 0.0
        normalized_amenities = [(a or "").strip().lower() for a in (amenities or [])]
        for a_key, val in AMENITY_VALUES.items():
            if any(a_key in item for item in normalized_amenities):
                amenities_val += val

        raw_estimated = area_cost + amenities_val
        estimated = round(raw_estimated / 50000.0) * 50000.0

        diff_percent = round(((price - estimated) / estimated) * 100, 1)

        district_name = district or "khu vực này"
        if diff_percent <= -8.0:
            label = "GIÁ TỐT"
            explanation = (
                f"Giá thuê {price:,.0f} đ/tháng thấp hơn khoảng {abs(diff_percent):.0f}% "
                f"so với mức định giá {estimated:,.0f} đ dựa trên diện tích {area:.0f}m² và tiện ích tại {district_name}."
            )
        elif diff_percent <= 8.0:
            label = "GIÁ HỢP LÝ"
            explanation = (
                f"Giá thuê {price:,.0f} đ/tháng sát với mức định giá {estimated:,.0f} đ "
                f"của hệ thống cho diện tích {area:.0f}m² và tiện nghi tại {district_name}."
            )
        else:
            label = "GIÁ CAO"
            explanation = (
                f"Giá thuê {price:,.0f} đ/tháng cao hơn khoảng {diff_percent:.0f}% "
                f"so với mức chuẩn {estimated:,.0f} đ tại {district_name}."
            )

        return {
            "label": label,
            "diff_percent": diff_percent,
            "estimated_price": estimated,
            "explanation": explanation,
        }


pricing_service = PricingService()
