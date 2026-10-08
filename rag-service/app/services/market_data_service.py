import logging
from pathlib import Path
from typing import List, Optional, Union

from app.config import settings
from app.services.market_evaluator import MarketEvaluator

logger = logging.getLogger(__name__)


class MarketDataService:
    """Loads the optional market CSV once and exposes room-price comparisons."""

    def __init__(self):
        self._evaluator: Optional[MarketEvaluator] = None
        self._loaded = False

    def evaluate_room(
        self,
        price: Optional[Union[float, int, str]],
        district: str,
        amenities: Optional[Union[List[str], str]] = None,
        area_sqm: Optional[Union[float, int, str]] = None,
        title: str = "",
    ) -> str:
        evaluator = self._get_evaluator()
        if evaluator is None or price is None:
            return ""

        price_text = f"{price:,.0f} đồng/tháng" if isinstance(price, (float, int)) else str(price)
        amenities_text = ", ".join(amenities) if isinstance(amenities, list) else (amenities or "")
        area_text = f"{area_sqm} m²" if area_sqm is not None else ""

        try:
            evaluation = evaluator.evaluate_room(
                gia_thue=price_text,
                quan_huyen=district or "",
                tien_nghi=amenities_text,
                dien_tich=area_text,
                tieu_de=title,
            )
            similar_rooms = evaluator.find_similar_rooms(district or "", price_text)
            return "\n".join(part for part in (evaluation, similar_rooms) if part)
        except Exception:
            logger.exception("Could not evaluate price against the configured market CSV.")
            return ""

    def _get_evaluator(self) -> Optional[MarketEvaluator]:
        if self._loaded:
            return self._evaluator

        self._loaded = True
        if not settings.MARKET_DATA_CSV:
            return None

        csv_path = Path(settings.MARKET_DATA_CSV)
        if not csv_path.is_absolute():
            service_root = Path(__file__).resolve().parents[2]
            csv_path = service_root / csv_path

        if not csv_path.is_file():
            logger.info("Market CSV not found at %s; dataset-based comparisons are disabled.", csv_path)
            return None

        try:
            self._evaluator = MarketEvaluator(str(csv_path))
            logger.info("Market evaluator loaded from %s", csv_path)
        except Exception:
            logger.exception("Could not load market CSV at %s", csv_path)
        return self._evaluator


market_data_service = MarketDataService()
