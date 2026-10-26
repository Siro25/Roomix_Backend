"""Các endpoint HTTP của Face Auth; không chứa xử lý mô hình AI."""
from fastapi import APIRouter, Header, HTTPException, Request, Response
from fastapi.concurrency import run_in_threadpool
from src.face_auth.api.schemas import ChallengeRequest, ChallengeResponse, FaceResponse
from src.face_auth.ai.image_input import MAX_IMAGE_BYTES
from src.face_auth.integrations.core_client import CoreUnavailable
from src.face_auth.services.face_workflow import WorkflowError
from src.face_auth.db.storage import DatabaseUnavailable
from src.face_auth.ai.errors import FaceUnavailable

router = APIRouter()


@router.get("/api/face/challenges/{challenge_id}", response_model=FaceResponse)
def challenge_status(challenge_id: str, request: Request, response: Response):
    response.headers["Cache-Control"] = "no-store"
    try:
        return request.app.state.workflow.status(challenge_id)
    except WorkflowError as exc:
        raise HTTPException(exc.status_code, exc.reason) from exc
    except DatabaseUnavailable as exc:
        raise HTTPException(503, "DATABASE_UNAVAILABLE") from exc

@router.get("/health")
def health():
    return {"status": "ok", "service": "face-auth-service"}

@router.post("/api/face/challenges", response_model=ChallengeResponse)
def create_challenge(data: ChallengeRequest, request: Request, authorization: str | None = Header(None)):
    try:
        return request.app.state.workflow.start(data.authSessionId, data.purpose, authorization)
    except WorkflowError as exc:
        raise HTTPException(exc.status_code, exc.reason) from exc
    except CoreUnavailable as exc:
        raise HTTPException(503, "CORE_UNAVAILABLE") from exc
    except DatabaseUnavailable as exc:
        raise HTTPException(503, "DATABASE_UNAVAILABLE") from exc

@router.post("/api/face/challenges/{challenge_id}/frames", response_model=FaceResponse)
async def submit_frame(challenge_id: str, request: Request):
    # Đọc có giới hạn cả khi client không gửi Content-Length.
    content = bytearray()
    async for chunk in request.stream():
        content.extend(chunk)
        if len(content) > MAX_IMAGE_BYTES:
            raise HTTPException(413, "IMAGE_TOO_LARGE")
    try:
        return await run_in_threadpool(
            request.app.state.workflow.process_frame, challenge_id, bytes(content))
    except WorkflowError as exc:
        raise HTTPException(exc.status_code, exc.reason) from exc
    except CoreUnavailable as exc:
        raise HTTPException(503, "CORE_UNAVAILABLE") from exc
    except DatabaseUnavailable as exc:
        raise HTTPException(503, "DATABASE_UNAVAILABLE") from exc
    except FaceUnavailable as exc:
        raise HTTPException(503, "AI_UNAVAILABLE") from exc

