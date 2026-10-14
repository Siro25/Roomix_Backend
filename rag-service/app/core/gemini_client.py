import hashlib
import logging

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import Runnable
from langchain_google_genai import ChatGoogleGenerativeAI, GoogleGenerativeAIEmbeddings

from app.config import settings

logger = logging.getLogger(__name__)


class GeminiClient:
    def __init__(self):
        api_key = settings.GEMINI_API_KEY.strip()
        if not api_key:
            raise ValueError("Cần cấu hình GEMINI_API_KEY trong .env để khởi động service.")


        self.model = ChatGoogleGenerativeAI(
            model=settings.GEMINI_MODEL,
            api_key=api_key,
            vertexai=False,
            max_retries=2,
        )

        self.embeddings = GoogleGenerativeAIEmbeddings(
            model=settings.GEMINI_EMBEDDING_MODEL,
            api_key=api_key,
            vertexai=False,
        )

        # thay đổi namespace dựa trên model embedding để tránh xung đột khi thay đổi model
        model_id = settings.GEMINI_EMBEDDING_MODEL.removeprefix("models/")
        model_hash = hashlib.sha256(model_id.encode("utf-8")).hexdigest()[:12]

        self.index_namespace = f"roomix_lc_v1_google_{model_hash}"

        logger.info("LangChain Gemini đã khởi tạo ")

    # Nối các xử lí từ prompt -> model -> output parser(lấy nội dung từ kết quả của model) để tạo thành một chain hoàn chỉnh
    # Để sinh câu trả lời cho user
    def create_chain(self, prompt: ChatPromptTemplate) -> Runnable:
        return prompt | self.model | StrOutputParser()


gemini_client = GeminiClient()
