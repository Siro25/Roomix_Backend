"""Các tên camelCase giữ giống hợp đồng API với Core Java."""
from typing import Literal
from pydantic import BaseModel, ConfigDict, Field


class ChallengeRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    authSessionId: str = Field(min_length=16, max_length=128)
    purpose: Literal["LOGIN", "ENROLL"]


class ChallengeResponse(BaseModel):
    challengeId: str
    actions: list[str]
    expiresAt: float


class FaceResponse(BaseModel):
    status: Literal["IN_PROGRESS", "SUCCEEDED", "FAILED"]
    action: str | None = None
    reason: str | None = None
