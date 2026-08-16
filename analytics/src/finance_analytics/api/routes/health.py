"""`GET /health` (PR-013 §3) — liveness check, no analytics engine involved."""

from __future__ import annotations

from fastapi import APIRouter

from finance_analytics.api.schemas import HealthResponse

router = APIRouter(tags=["health"])


@router.get(
    "/health",
    response_model=HealthResponse,
    summary="Liveness check",
    description='Returns 200 with `{"status": "ok"}` whenever the API process is up. '
    "Does not touch the analytics engine and never fails on its own.",
)
def get_health() -> HealthResponse:
    return HealthResponse(status="ok")
