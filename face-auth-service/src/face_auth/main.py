"""Chạy: python -m uvicorn src.face_auth.main:app --port 8003"""
from contextlib import asynccontextmanager
import asyncio
from pathlib import Path
from fastapi import FastAPI
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from src.face_auth.api.routes import router
from fastapi.concurrency import run_in_threadpool
from src.face_auth.config import Settings
from src.face_auth.integrations.core_client import CoreClient
from src.face_auth.db.storage import Store
from src.face_auth.integrations.telegram_notifier import TelegramNotifier
from src.face_auth.services.face_workflow import FaceWorkflow
from src.face_auth.services.telegram_worker import TelegramWorker


def create_app(settings=None, core=None, analyzer=None, notifier=None, store=None):
    """Cho phép thay adapter bằng fake khi kiểm thử, không tải AI hay gửi Telegram."""
    @asynccontextmanager
    async def lifespan(application):
        config = settings or Settings.from_env()
        application.state.enable_demo = config.enable_demo
        core_client = core or CoreClient(config)
        telegram = notifier or TelegramNotifier(config)
        stop_worker = asyncio.Event()
        worker_task = None
        try:
            face_store = store or Store(config.database_url)
            if analyzer is None:
                from src.face_auth.ai.face_service import FaceService
                face_analyzer = await run_in_threadpool(FaceService.get_instance)
            else:
                face_analyzer = analyzer
            application.state.workflow = FaceWorkflow(
                config, face_store, core_client, face_analyzer)
            if config.telegram_token and config.telegram_chat_id:
                worker_task = asyncio.create_task(TelegramWorker(face_store, telegram).run(stop_worker))
            yield
        finally:
            stop_worker.set()
            if worker_task:
                await worker_task
            core_client.close()
            telegram.close()

    application = FastAPI(title="Roomix Face Auth", version="1.0.0", lifespan=lifespan)

    @application.middleware("http")
    async def prevent_face_response_caching(request, call_next):
        response = await call_next(request)
        if request.url.path.startswith("/api/face/"):
            response.headers["Cache-Control"] = "no-store"
        return response

    application.include_router(router)

    # Chỉ bật công cụ thử nghiệm khi cấu hình rõ ràng, không mở mặc định.
    demo_enabled = settings.enable_demo if settings else None
    if demo_enabled is None:
        import os
        demo_enabled = os.environ.get("FACE_ENABLE_DEMO", "false").lower() == "true"
    if demo_enabled:
        web_dir = Path(__file__).with_name("web")
        application.mount("/demo/assets", StaticFiles(directory=web_dir), name="demo-assets")

        @application.get("/demo", include_in_schema=False)
        def demo():
            return FileResponse(web_dir / "index.html", headers={
                "Cache-Control": "no-store",
                "Referrer-Policy": "no-referrer",
                "Content-Security-Policy": "default-src 'self'; img-src 'self' blob:; media-src 'self' blob:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'",
            })

    return application


app = create_app()
