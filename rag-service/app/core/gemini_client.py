import logging
import hashlib
from typing import List, Optional
from app.config import settings

logger = logging.getLogger(__name__)

class GeminiClient:
    def __init__(self):
        self.api_key = settings.GEMINI_API_KEY
        self.model_name = settings.GEMINI_MODEL
        self.embedding_model = settings.GEMINI_EMBEDDING_MODEL
        self._is_configured = False
        self._client = None

        if self.api_key and self.api_key != "your_gemini_api_key_here":
            try:
                # Use official google-genai SDK
                from google import genai
                self._client = genai.Client(api_key=self.api_key)
                self._is_configured = True
                logger.info(f"Gemini Client configured successfully with model: {self.model_name}")
            except Exception as e:
                logger.warning(f"Failed to initialize google-genai, trying legacy SDK: {e}")
                try:
                    import google.generativeai as legacy_genai
                    legacy_genai.configure(api_key=self.api_key)
                    self._legacy_model = legacy_genai.GenerativeModel(self.model_name)
                    self._is_configured = True
                    logger.info("Initialized with legacy google.generativeai SDK.")
                except Exception as ex:
                    logger.error(f"Cannot configure Gemini client: {ex}")
        else:
            logger.info("GEMINI_API_KEY is not provided. Running in dev/mock mode.")

    def is_available(self) -> bool:
        return self._is_configured

    def generate_content(self, prompt: str, system_instruction: Optional[str] = None) -> str:
        """Sinh nội dung văn bản từ prompt sử dụng Gemini."""
        if not self._is_configured:
            return (
                "⚠️ [Chế độ thử nghiệm - Chưa cấu hình GEMINI_API_KEY]:\n"
                "Hệ thống đã nhận diện câu hỏi của bạn và tìm kiếm thành công dữ liệu phòng trọ/chính sách từ Vector Store.\n"
                "Vui lòng thiết lập biến môi trường `GEMINI_API_KEY` trong file `.env` để kích hoạt phản hồi hoàn chỉnh từ Gemini AI."
            )
        try:
            if self._client:
                from google.genai import types
                config = types.GenerateContentConfig(
                    system_instruction=system_instruction
                ) if system_instruction else None
                response = self._client.models.generate_content(
                    model=self.model_name,
                    contents=prompt,
                    config=config
                )
                return response.text
            elif hasattr(self, "_legacy_model"):
                response = self._legacy_model.generate_content(prompt)
                return response.text
        except Exception as e:
            logger.error(f"Error calling Gemini generate_content: {e}")
            return f"Xin lỗi, đã xảy ra lỗi khi kết nối với Gemini AI: {str(e)}"

    def get_embedding(self, text: str, task_type: str = "retrieval_query") -> List[float]:
        """Tạo vector embedding cho văn bản."""
        if self._is_configured and self._client:
            try:
                response = self._client.models.embed_content(
                    model="text-embedding-004",
                    contents=text
                )
                if hasattr(response, "embedding") and response.embedding:
                    return response.embedding.values
                if hasattr(response, "embeddings") and response.embeddings:
                    return response.embeddings[0].values
            except Exception as e:
                logger.warning(f"Error getting embedding from Gemini ({e}), falling back to deterministic embedding.")

        # Fallback deterministic pseudo-embedding (768 dimensions) for test/dev
        import numpy as np
        seed = int(hashlib.md5(text.encode("utf-8")).hexdigest(), 16)
        np.random.seed(seed % (2**32))
        vec = np.random.uniform(-1, 1, 768).tolist()
        norm = sum(x*x for x in vec) ** 0.5
        return [x / norm for x in vec]


gemini_client = GeminiClient()
