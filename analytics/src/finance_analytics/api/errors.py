"""Exception -> HTTP response mapping (PR-013 §7, §10).

Registered once by `app.create_app`. Two handlers cover the contract beyond
FastAPI's own built-in 422 (Pydantic `RequestValidationError`):

- `TransactionValidationError` -> 400: a request that is well-typed per
  the Pydantic schema but structurally invalid per
  `finance_analytics.validation.transactions` (PR-013 §5 note in
  `service.py`).
- Anything else unhandled -> 500: an analytics-engine failure. The
  exception and its traceback are logged server-side for debugging
  (`logging.Logger.exception`) — the request body/transaction payload is
  never passed to the logger, so it cannot appear in that traceback's local
  variables. The client only ever sees a fixed, detail-free message — no
  stack trace, no transaction data (PR-013 §7, §16).
"""

from __future__ import annotations

import logging

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from finance_analytics.api.service import TransactionValidationError

logger = logging.getLogger(__name__)

_INTERNAL_ERROR_DETAIL = "Internal analytics failure."


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(TransactionValidationError)
    async def handle_transaction_validation_error(
        request: Request, exc: TransactionValidationError
    ) -> JSONResponse:
        del request
        return JSONResponse(status_code=400, content={"detail": str(exc)})

    @app.exception_handler(Exception)
    async def handle_unexpected_error(request: Request, exc: Exception) -> JSONResponse:
        logger.exception(
            "Unhandled error processing %s %s: %s",
            request.method,
            request.url.path,
            type(exc).__name__,
        )
        return JSONResponse(status_code=500, content={"detail": _INTERNAL_ERROR_DETAIL})
