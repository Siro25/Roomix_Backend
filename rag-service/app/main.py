import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import settings
from app.api import api_router
from app.api.routes_ingest import init_sample_data
from app.core.vector_store import vector_store

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - [%(levelname)s] - %(name)s - %(message)s"
)
logger = logging.getLogger("roomix-rag")


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting Roomix RAG Service...")
    # Auto-load sample data on startup if collections are empty
    try:
        room_count = vector_store.rooms_col.count()
        if room_count == 0:
            logger.info("Vector database is empty. Auto-indexing sample rooms and policies...")
            init_sample_data()
        else:
            logger.info(f"Vector database already has {room_count} room records.")
    except Exception as e:
        logger.warning(f"Could not check or init sample data on startup: {e}")
    yield
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
    allow_credentials=True,
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
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.SERVICE_HOST,
        port=settings.SERVICE_PORT,
        reload=True
    )
