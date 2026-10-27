import asyncio
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import settings
from app.api import api_router
from app.api.routes_ingest import seed_policies, seed_sample_rooms
from app.core.vector_store import vector_store
from app.services.core_room_sync import core_room_sync

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - [%(levelname)s] - %(name)s - %(message)s"
)
logger = logging.getLogger("roomix-rag")


async def _room_sync_loop():
    """Keep the room index in step with core-service (the source of truth)."""
    interval = settings.CORE_SYNC_INTERVAL_SECONDS
    while True:
        try:
            await asyncio.to_thread(core_room_sync.sync)
            delay = interval
        except Exception as e:
            logger.warning(f"Room sync failed, will retry soon: {e}")
            delay = min(interval, 30)
        await asyncio.sleep(delay)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting Roomix RAG Service...")
    # Re-embed stored documents if the embedding model/mode changed
    try:
        vector_store.ensure_embeddings_current()
    except Exception as e:
        logger.error(f"Could not verify/refresh embeddings on startup: {e}")
    # Static legal/policy knowledge base
    try:
        if vector_store.policies_col.count() == 0:
            logger.info("Indexing rental policies...")
            seed_policies()
    except Exception as e:
        logger.warning(f"Could not index policies on startup: {e}")

    sync_task = None
    if core_room_sync.enabled:
        logger.info(f"Room data source: core-service at {settings.CORE_SERVICE_URL}")
        sync_task = asyncio.create_task(_room_sync_loop())
    elif settings.SEED_SAMPLE_ROOMS:
        try:
            if vector_store.rooms_col.count() == 0:
                logger.warning("CORE_SERVICE_URL not set: indexing DEMO sample rooms.")
                seed_sample_rooms()
        except Exception as e:
            logger.warning(f"Could not index sample rooms on startup: {e}")
    else:
        logger.warning("CORE_SERVICE_URL not set and SEED_SAMPLE_ROOMS disabled: no room data will be indexed.")
    yield
    if sync_task:
        sync_task.cancel()
    logger.info("Roomix RAG Service is shutting down...")


app = FastAPI(
    title="Roomix RAG Service",
    description="Dịch vụ AI RAG phân tích dự đoán định giá & trợ lý tư vấn khách hàng cho hệ thống cho thuê phòng trọ Roomix",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    # Browsers reject credentials with a wildcard origin.
    allow_credentials="*" not in settings.CORS_ORIGINS,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(api_router)


@app.get("/health", tags=["Health"])
def health_check():
    return {
        "status": "UP",
        "service": "roomix-rag-service",
        "vector_db_rooms_count": vector_store.rooms_col.count(),
        "vector_db_policies_count": vector_store.policies_col.count(),
        "room_sync": {"enabled": core_room_sync.enabled, **core_room_sync.last_status},
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.SERVICE_HOST,
        port=settings.SERVICE_PORT,
        reload=True
    )
