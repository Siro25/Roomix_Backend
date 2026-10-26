from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field


class ChatMessage(BaseModel):
    role: str = Field(..., description="'user', 'assistant' or 'system'")
    content: str


class SearchFilters(BaseModel):
    city: Optional[str] = None
    district: Optional[str] = None
    min_price: Optional[float] = None
    max_price: Optional[float] = None
    min_area: Optional[float] = None
    max_area: Optional[float] = None
    room_type: Optional[str] = None  # PHONG_TRO, CHUNG_CU_MINI, CAN_HO_DICH_VU, NHA_NGUYEN_CAN, O_GHEP
    amenities: Optional[List[str]] = None


class ChatRequest(BaseModel):
    message: str = Field(..., description="Nội dung câu hỏi của người dùng")
    conversation_history: Optional[List[ChatMessage]] = Field(default_factory=list)
    user_role: Optional[str] = Field("TENANT", description="TENANT hoặc LANDLORD")
    filters: Optional[SearchFilters] = None


class RoomItem(BaseModel):
    id: str
    title: str
    description: Optional[str] = ""
    room_type: str = "PHONG_TRO"
    address: str
    ward: Optional[str] = ""
    district: str
    city: str
    area_sqm: float
    price: float  # VND/tháng
    deposit: Optional[float] = None
    electricity_cost: Optional[float] = None  # VND/kWh
    water_cost: Optional[float] = None        # VND/khối hoặc người
    amenities: List[str] = Field(default_factory=list)
    rules: Optional[List[str]] = Field(default_factory=list)
    contact_phone: Optional[str] = ""
    landlord_name: Optional[str] = ""
    available: bool = True


class ChatResponse(BaseModel):
    answer: str
    matched_rooms: List[Dict[str, Any]] = Field(default_factory=list)
    source_documents: List[str] = Field(default_factory=list)
    suggested_follow_ups: List[str] = Field(default_factory=list)


class IngestRoomsRequest(BaseModel):
    rooms: List[RoomItem]


class IngestResponse(BaseModel):
    success: bool
    indexed_count: int
    message: str


# Pricing prediction models
class PricingPredictRequest(BaseModel):
    city: str = Field(..., example="TP Hồ Chí Minh")
    district: str = Field(..., example="Quận 7")
    ward: Optional[str] = Field(None, example="Tân Phong")
    room_type: str = Field("PHONG_TRO", description="PHONG_TRO, CHUNG_CU_MINI, CAN_HO_DICH_VU, NHA_NGUYEN_CAN")
    area_sqm: float = Field(..., gt=5, description="Diện tích tính bằng mét vuông")
    amenities: List[str] = Field(default_factory=list, description="Danh sách tiện ích: MayLanh, TuLanh, MayGiat, BanCong, ThangMay, v.v.")
    is_mezzanine: bool = Field(False, description="Có gác lửng")
    is_studio: bool = Field(False, description="Phòng dạng Studio / khép kín cao cấp")
    has_parking: bool = Field(True, description="Có chỗ để xe riêng")
    is_newly_built: bool = Field(False, description="Phòng mới xây / mới cải tạo")
    floor: Optional[int] = Field(None, description="Tầng lầu")


class PricingFactorBreakdown(BaseModel):
    base_price_per_sqm: float
    district_weight: float
    area_cost: float
    amenities_surcharge: float
    feature_bonus: float


class PricingPredictResponse(BaseModel):
    recommended_price: float = Field(..., description="Giá thuê tối ưu đề xuất (VND/tháng)")
    estimated_price_min: float = Field(..., description="Giá thuê cạnh tranh thấp nhất")
    estimated_price_max: float = Field(..., description="Giá thuê tối đa có thể đạt được")
    confidence_score: float = Field(..., ge=0.0, le=1.0)
    breakdown: PricingFactorBreakdown
    market_analysis: str = Field(..., description="Phân tích thị trường, phân khúc khách thuê và gợi ý tối ưu từ AI")
    comparable_rooms: List[Dict[str, Any]] = Field(default_factory=list)
