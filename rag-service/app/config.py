from typing import List
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    GEMINI_API_KEY: str = ""
    GEMINI_MODEL: str = "gemini-3.6-flash"
    # text-embedding-004 has been retired; gemini-embedding-001 supports MRL so
    # we keep 768 dimensions to stay compact.
    GEMINI_EMBEDDING_MODEL: str = "gemini-embedding-001"
    GEMINI_EMBEDDING_DIM: int = 768

    SERVICE_PORT: int = 8083
    SERVICE_HOST: str = "0.0.0.0"
    CORS_ORIGINS: List[str] = ["*"]

    CHROMA_PERSIST_DIRECTORY: str = "./chroma_db"

    # Room data comes from core-service (the system's real room database).
    # Leave CORE_SERVICE_URL empty to disable syncing (e.g. isolated dev runs).
    CORE_SERVICE_URL: str = ""
    CORE_SYNC_INTERVAL_SECONDS: int = 300
    CORE_SYNC_PAGE_SIZE: int = 100
    CORE_SYNC_TIMEOUT_SECONDS: float = 15.0
    # Fake demo rooms; only used when CORE_SERVICE_URL is empty.
    SEED_SAMPLE_ROOMS: bool = False

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )


settings = Settings()
