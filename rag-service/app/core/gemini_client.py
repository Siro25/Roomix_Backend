import logging
import hashlib
import math
import re
from typing import List, Optional
from app.config import settings

logger = logging.getLogger(__name__)

LOCAL_EMBEDDING_DIM = 768
_EMBED_BATCH_SIZE = 50
_TASK_TYPES = {
    "retrieval_query": "RETRIEVAL_QUERY",
    "retrieval_document": "RETRIEVAL_DOCUMENT",
}


class EmbeddingError(RuntimeError):
    """Raised when a configured Gemini embedding call fails.

    We deliberately do not fall back to local vectors in that case: mixing
    vectors from different embedding spaces in one collection silently breaks
    semantic search.
    """


def _l2_normalize(vec: List[float]) -> List[float]:
    norm = math.sqrt(sum(x * x for x in vec))
    if norm == 0:
        return list(vec)
    return [x / norm for x in vec]


def local_embedding(text: str, dim: int = LOCAL_EMBEDDING_DIM) -> List[float]:
    """Deterministic offline embedding using hashed unigrams + bigrams.

    Not semantic, but texts that share words end up close together, so dev/test
    mode (no API key) still returns sensible lexical matches.
    """
    tokens = re.findall(r"\w+", (text or "").lower())
    features = tokens + [f"{a}_{b}" for a, b in zip(tokens, tokens[1:])]
    vec = [0.0] * dim
    for feature in features:
        digest = hashlib.md5(feature.encode("utf-8")).digest()
        idx = int.from_bytes(digest[:4], "little") % dim
        sign = 1.0 if digest[4] & 1 else -1.0
        vec[idx] += sign
    if not any(vec):
        vec[0] = 1.0
    return _l2_normalize(vec)


class GeminiClient:
    def __init__(self):
        self.api_key = settings.GEMINI_API_KEY
        self.model_name = settings.GEMINI_MODEL
        self.embedding_model = settings.GEMINI_EMBEDDING_MODEL
        self.embedding_dim = settings.GEMINI_EMBEDDING_DIM
        self._is_configured = False
        self._client = None
        self._legacy_genai = None

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
                    self._legacy_genai = legacy_genai
                    self._is_configured = True
                    logger.info("Initialized with legacy google.generativeai SDK.")
                except Exception as ex:
                    logger.error(f"Cannot configure Gemini client: {ex}")
        else:
            logger.info("GEMINI_API_KEY is not provided. Running in dev/mock mode.")

    def is_available(self) -> bool:
        return self._is_configured

    @property
    def embedding_signature(self) -> str:
        """Identifies the embedding space; stored with the vector collections."""
        if self._is_configured:
            return f"gemini:{self.embedding_model}:{self.embedding_dim}"
        return f"local-hash:{LOCAL_EMBEDDING_DIM}"

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
                return response.text or ""
            if self._legacy_genai is not None:
                model = self._legacy_genai.GenerativeModel(
                    self.model_name,
                    system_instruction=system_instruction,
                )
                return model.generate_content(prompt).text or ""
        except Exception as e:
            logger.error(f"Error calling Gemini generate_content: {e}", exc_info=True)
            return "Xin lỗi, hiện không thể kết nối tới Gemini AI. Vui lòng thử lại sau ít phút."
        return "Xin lỗi, Gemini AI hiện chưa sẵn sàng."

    def get_embedding(self, text: str, task_type: str = "retrieval_query") -> List[float]:
        """Tạo vector embedding cho một văn bản."""
        return self.get_embeddings([text], task_type=task_type)[0]

    def get_embeddings(self, texts: List[str], task_type: str = "retrieval_document") -> List[List[float]]:
        """Tạo vector embedding cho nhiều văn bản (gọi API theo lô)."""
        if not texts:
            return []
        if not (self._is_configured and self._client):
            return [local_embedding(t) for t in texts]

        from google.genai import types
        config = types.EmbedContentConfig(
            task_type=_TASK_TYPES.get(task_type, "RETRIEVAL_DOCUMENT"),
            output_dimensionality=self.embedding_dim,
        )
        vectors: List[List[float]] = []
        for start in range(0, len(texts), _EMBED_BATCH_SIZE):
            batch = texts[start:start + _EMBED_BATCH_SIZE]
            try:
                response = self._client.models.embed_content(
                    model=self.embedding_model,
                    contents=batch,
                    config=config,
                )
                embeddings = response.embeddings or []
            except Exception as e:
                logger.error(f"Gemini embedding call failed: {e}", exc_info=True)
                raise EmbeddingError(f"Gemini embedding failed: {e}") from e
            if len(embeddings) != len(batch):
                raise EmbeddingError(
                    f"Gemini returned {len(embeddings)} embeddings for {len(batch)} inputs."
                )
            # Truncated (MRL) vectors are not unit-length, so normalise them.
            vectors.extend(_l2_normalize(list(e.values)) for e in embeddings)
        return vectors


gemini_client = GeminiClient()
