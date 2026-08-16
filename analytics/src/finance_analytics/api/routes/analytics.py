"""`POST /analytics/analyse` (PR-013 §4) — transport only.

This handler does no analysis itself: it hands the parsed, schema-validated
request straight to `service.run_analysis` and returns its result.
Errors are not caught here — `api.errors` maps
`service.TransactionValidationError` to 400 and any other exception to 500
at the application level, so every route gets the same handling for free.
"""

from __future__ import annotations

from fastapi import APIRouter

from finance_analytics.api.schemas import AnalysisRequest, AnalysisResponse, ErrorResponse
from finance_analytics.api.service import run_analysis

router = APIRouter(tags=["analytics"])


@router.post(
    "/analyse",
    response_model=AnalysisResponse,
    summary="Run the full analytics + insights pipeline over a transaction dataset",
    description="Accepts a transaction dataset and orchestrates the existing, "
    "already-validated analytics engine (enrichment, anomaly detection, recurring "
    "detection, insights) over it. Stateless: nothing sent here is persisted, and "
    "nothing about the request is logged.",
    responses={
        # 422 is intentionally left to FastAPI's own default documentation
        # (`HTTPValidationError`) — that is the actual shape Pydantic's
        # `RequestValidationError` produces (`{"detail": [...]}`, one entry
        # per invalid field), which `ErrorResponse.detail: str` does not
        # match. Documenting it here would make the OpenAPI schema lie
        # about the real response shape (PR-013 §11).
        400: {"model": ErrorResponse, "description": "Structurally invalid transaction data."},
        500: {"model": ErrorResponse, "description": "Internal analytics failure."},
    },
)
def analyse(request: AnalysisRequest) -> AnalysisResponse:
    return run_analysis(request)
