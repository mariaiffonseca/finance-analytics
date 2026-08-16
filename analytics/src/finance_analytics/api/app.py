"""FastAPI application factory (PR-013).

Run locally with:

    cd analytics
    uv run uvicorn finance_analytics.api.app:app --reload

See `analytics/README.md` for full local-development instructions,
including the `/docs` and `/openapi.json` URLs.
"""

from __future__ import annotations

from fastapi import FastAPI

from finance_analytics.api.errors import register_exception_handlers
from finance_analytics.api.routes import analytics, health

_DESCRIPTION = """
Orchestration/transport layer over the Finance Analytics engine
(merchant normalisation & categorisation, anomaly detection, recurring-transaction
detection, and the Insights Engine).

**Stateless.** Nothing sent to this API is persisted, logged, or retained between
requests — the caller sends the transactions to analyse on every call.

**Local development only.** No authentication is implemented yet; do not expose
this service outside a trusted local environment, and do not send real financial
data to it. See `analytics/README.md` for details.
"""


def create_app() -> FastAPI:
    app = FastAPI(
        title="Finance Analytics API",
        description=_DESCRIPTION,
        version="0.1.0",
    )
    register_exception_handlers(app)
    app.include_router(health.router)
    app.include_router(analytics.router, prefix="/analytics")
    return app


app = create_app()
