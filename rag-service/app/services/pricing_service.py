import json
import logging
from typing import Dict, Any, List
from app.models.schemas import PricingPredictRequest, PricingPredictResponse, PricingFactorBreakdown
from app.core.vector_store import vector_store
from app.core.gemini_client import gemini_client
from app.services.market_data_service import market_data_service

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

        # Add a data-backed comparison when a market CSV has been configured.
        market_evaluation = self._evaluate_against_market_data(req, recommended)

        # Market Analysis via Gemini
        market_analysis = self._generate_ai_market_analysis(
            req, recommended, min_price, max_price, comparables, market_evaluation
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
        market_evaluation: str = ""
    ) -> str:
        prompt = f"""
Bạn là chuyên gia thẩm định giá và tư vấn tối ưu vận hành bất động sản cho thuê (phòng trọ, căn hộ mini) tại Việt Nam của nền tảng Roomix.
Hãy phân tích cơ hội định giá và chiến lược cho thuê căn phòng sau cho chủ trọ:

THÔNG TIN PHÒNG CỦA CHỦ TRỌ:
- Khu vực: {req.district}, {req.city} (Phường: {req.ward or 'Trung tâm'})
- Loại hình: {req.room_type}
- Diện tích: {req.area_sqm} m2
- Tiện ích hiện có: {', '.join(req.amenities) if req.amenities else 'Cơ bản'}
- Gác lửng: {'Có' if req.is_mezzanine else 'Không'}
- Dạng Studio: {'Có' if req.is_studio else 'Không'}
- Mới xây/cải tạo: {'Mới xây' if req.is_newly_built else 'Cũ'}

KẾT QUẢ TÍNH TOÁN DỰ ĐOÁN:
- Giá đề xuất tối ưu: {recommended:,.0f} VND/tháng
- Khung giá thị trường: {min_p:,.0f} - {max_p:,.0f} VND/tháng
- Các phòng tương đương tham khảo trong khu vực: {json.dumps(comparables, ensure_ascii=False)}

SO SÁNH TỪ DỮ LIỆU THỊ TRƯỜNG ĐÃ THU THẬP:
{market_evaluation or "Chưa có CSV dữ liệu thị trường được cấu hình."}

YÊU CẦU ĐẦU RA:
1. Đánh giá tính cạnh tranh của mức giá {recommended:,.0f} VND trong khu vực {req.district}.
2. Phân khúc khách hàng mục tiêu phù hợp nhất (Sinh viên, Dân văn phòng, Gia đình trẻ,...).
3. Dự báo tỷ lệ lấp đầy và thời gian trung bình tìm được khách thuê.
4. Gợi ý 2-3 nâng cấp chi phí thấp giúp chủ trọ có thể nâng giá thêm 300k - 500k/tháng.
Viết ngắn gọn, chuyên nghiệp, dùng định dạng Markdown với bullet points rõ ràng.
"""
        analysis = gemini_client.generate_content(prompt)
        return analysis

    def _evaluate_against_market_data(self, req: PricingPredictRequest, recommended: float) -> str:
        return market_data_service.evaluate_room(
            price=recommended,
            district=req.district,
            amenities=req.amenities,
            area_sqm=req.area_sqm,
        )


pricing_service = PricingService()
