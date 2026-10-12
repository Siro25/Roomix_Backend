"""Shared LangChain models and embeddings used throughout the RAG service."""

import hashlib

import numpy as np
from langchain_core.embeddings import Embeddings
from langchain_core.language_models.fake_chat_models import FakeListChatModel
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_google_genai import ChatGoogleGenerativeAI, GoogleGenerativeAIEmbeddings

from app.config import settings

_gemini_api_key = settings.GEMINI_API_KEY.strip()
_has_gemini_api_key = bool(
    _gemini_api_key and _gemini_api_key != "your_gemini_api_key_here"
)


class DevelopmentHashEmbeddings(Embeddings):
    """Deterministic LangChain embeddings for local runs without a Gemini key.

    This preserves the previous development-mode vector format. It is not
    semantically meaningful and should not be used for production retrieval.
    """

    def __init__(self, size: int = 768):
        self.size = size

    def _embed(self, text: str) -> list[float]:
        seed = int(hashlib.md5(text.encode("utf-8")).hexdigest(), 16)
        vector = np.random.RandomState(seed % (2**32)).uniform(-1, 1, self.size)
        norm = np.linalg.norm(vector)
        return (vector / norm).tolist()

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return [self._embed(text) for text in texts]

    def embed_query(self, text: str) -> list[float]:
        return self._embed(text)


if _has_gemini_api_key:
    embeddings: Embeddings = GoogleGenerativeAIEmbeddings(
        model=settings.GEMINI_EMBEDDING_MODEL,
        google_api_key=_gemini_api_key,
        # Existing persisted vectors were created without a task type. Keep
        # the same setting so they remain comparable during this migration.
        task_type="TASK_TYPE_UNSPECIFIED",
    )
else:
    embeddings = DevelopmentHashEmbeddings()


def create_chat_chain(system_prompt: str, fallback_response: str):
    """Build a LangChain prompt → chat model → text parser chain."""
    if _has_gemini_api_key:
        model = ChatGoogleGenerativeAI(
            model=settings.GEMINI_MODEL,
            api_key=_gemini_api_key,
            temperature=0.2,
        )
    else:
        model = FakeListChatModel(responses=[fallback_response])

    return (
        ChatPromptTemplate.from_messages([
            ("system", system_prompt),
            ("human", "{prompt}"),
        ])
        | model
        | StrOutputParser()
    )
