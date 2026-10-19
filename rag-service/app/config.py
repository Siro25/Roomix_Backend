import os
from typing import List
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    GEMINI_API_KEY: str = os.getenv("GEMINI_API_KEY", "")
    GEMINI_MODEL: str = os.getenv("GEMINI_MODEL", "gemini-3.6-flash")
    GEMINI_EMBEDDING_MODEL: str = os.getenv("GEMINI_EMBEDDING_MODEL", "models/text-embedding-004")

    SERVICE_PORT: int = int(os.getenv("SERVICE_PORT", "8083"))
    SERVICE_HOST: str = os.getenv("SERVICE_HOST", "0.0.0.0")
    CORS_ORIGINS: List[str] = ["*"]

    CHROMA_PERSIST_DIRECTORY: str = os.getenv("CHROMA_PERSIST_DIRECTORY", "./chroma_db")
    MARKET_DATA_CSV: str = os.getenv("MARKET_DATA_CSV", "data/market/phongtro_hanoi.csv")

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )


settings = Settings()
