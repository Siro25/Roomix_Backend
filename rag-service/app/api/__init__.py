from fastapi import APIRouter
from app.api.routes_chat import router as chat_router
from app.api.routes_pricing import router as pricing_router
from app.api.routes_ingest import router as ingest_router

api_router = APIRouter(prefix="/api/v1")
api_router.include_router(chat_router)
api_router.include_router(pricing_router)
api_router.include_router(ingest_router)
