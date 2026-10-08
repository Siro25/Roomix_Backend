import logging
from fastapi import APIRouter, HTTPException
from app.models.schemas import PricingPredictRequest, PricingPredictResponse
from app.services.pricing_service import pricing_service

router = APIRouter(prefix="/pricing", tags=["Rental Pricing AI"])
logger = logging.getLogger(__name__)

@router.post("/predict", response_model=PricingPredictResponse, summary="Dự đoán định giá phòng trọ và phân tích thị trường")
def predict_rental_price(request: PricingPredictRequest):
    """
    Điểm cuối hỗ trợ chủ trọ phân tích & dự đoán định giá phòng:
    - Tính toán giá thuê tối ưu dựa trên khu vực, diện tích, tiện ích
    - Cung cấp dải giá min-max cạnh tranh
    - Phân tích insight thị trường, chân dung khách thuê và mẹo gia tăng giá trị phòng
    """
    try:
        response = pricing_service.predict_rental_price(request)
        return response
    except Exception as e:
        logger.error(f"Error in pricing predict endpoint: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Lỗi tính toán định giá: {str(e)}")
