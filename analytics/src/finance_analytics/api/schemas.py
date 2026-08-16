"""Explicit Pydantic request/response schemas for the Analytics API (PR-013 §5-6).

These models are the API's own contract, not the analytics engine's
internal result types. `service.py` maps between the two so a change to an
internal dataclass (`AnomalyResult`, `RecurringResult`, `Insight`, ...)
cannot silently change the HTTP contract. Field constraints here
(`min_length`, required vs. optional) are structural/transport validation
only — never a business rule, threshold or analytical decision, which
belong to the analytics engine (PR-013 §15, Critical Architecture Rule).
"""

from __future__ import annotations

from datetime import date as date_type
from typing import Any

from pydantic import BaseModel, ConfigDict, Field

# --- Request ----------------------------------------------------------------


class TransactionRequest(BaseModel):
    """A single transaction, mirroring `docs/project/02_DOMAIN_MODEL.md`'s
    `Transaction` entity and `finance_analytics.data.schema.REQUIRED_COLUMNS`.

    `id`, `currency` and `merchant` are required to be non-blank here
    because `finance_analytics.data.schema.REQUIRED_VALUE_COLUMNS` requires
    them; `description`, `category` and `account` may legitimately be
    absent on a raw import (PR-007), so they stay optional.
    """

    model_config = ConfigDict(str_strip_whitespace=True)

    id: str = Field(min_length=1, description="Source transaction identifier.")
    date: date_type = Field(description="Transaction date (ISO 8601, e.g. 2026-03-05).")
    amount: float = Field(
        description="Signed transaction amount. Negative for an expense, positive for income."
    )
    currency: str = Field(min_length=1, description="ISO-style currency code, e.g. EUR.")
    description: str | None = Field(default=None, description="Free-text transaction memo.")
    merchant: str = Field(min_length=1, description="Raw merchant name as imported.")
    category: str | None = Field(default=None, description="Raw imported category, if any.")
    account: str | None = Field(default=None, description="Source account label, if any.")


class AnalysisRequest(BaseModel):
    """Request body for `POST /analytics/analyse`.

    `transactions` requires at least one entry: with zero transactions
    there is no dataset to analyse, so this is rejected as a 422 validation
    error rather than producing a hollow, all-empty response (PR-013 §12,
    "Empty transaction handling behaves as documented").
    """

    transactions: list[TransactionRequest] = Field(min_length=1)


# --- Response: shared analytical result shapes -------------------------------


class InsightResponse(BaseModel):
    """One `finance_analytics.insights.models.Insight`, unchanged in shape
    (`Insight.to_dict()` defines the same fields) — this is the API's typed
    mirror of that dict, not a reimplementation."""

    id: str
    type: str
    title: str
    description: str
    severity: str
    confidence: float
    related_transaction_ids: list[str]
    metadata: dict[str, Any]
    category: str | None
    merchant: str | None
    amount: float | None
    comparison_period: str | None


class AnomalyResponse(BaseModel):
    """One `finance_analytics.anomalies.detector.AnomalyResult` that was
    flagged `is_anomaly=True`. Unflagged/insufficient-history results are
    filtered out by `service.py` before this schema is built — they are not
    a finding worth returning (same convention `insights.conversions`
    already uses for anomaly-derived insights)."""

    transaction_id: str
    anomaly_score: float | None
    is_anomaly: bool
    method: str
    reason: str
    reference_context: dict[str, Any]


class RecurringResponse(BaseModel):
    """One `finance_analytics.recurring.detector.RecurringResult` classified
    `"Recurring"` or `"Possible recurring"`. `"Not recurring"` and
    `"Insufficient history"` candidates are filtered out by `service.py` —
    same convention as `insights.conversions.recurring_payment_insights`."""

    merchant: str
    currency: str
    is_recurring: bool
    classification: str
    confidence_score: float | None
    frequency: str
    occurrences: int
    median_amount: float | None
    amount_variation: float | None
    median_interval_days: float | None
    interval_variation: float | None
    first_seen: date_type
    last_seen: date_type
    reason: str
    transaction_ids: list[str]


# --- Response: summary / metadata --------------------------------------------


class AnalysisSummary(BaseModel):
    """Headline financial totals for the analysed dataset.

    Built entirely from existing PR-007 building blocks
    (`finance_analytics.duckdb_queries.total_income`/`total_expenses` and
    `finance_analytics.data.quality.build_quality_report`) — no new
    aggregation logic is introduced by the API (PR-013 §15).
    """

    transaction_count: int = Field(description="Distinct transactions analysed, after dedup by id.")
    income_count: int
    expense_count: int
    total_income: float
    total_expenses: float
    net_savings: float = Field(description="total_income + total_expenses (expenses are negative).")
    date_range_start: date_type | None
    date_range_end: date_type | None
    unique_merchant_count: int
    unique_category_count: int


class AnalysisMetadata(BaseModel):
    """Diagnostic/run metadata, distinct from the financial `summary`.

    `requested_transaction_count` vs. `processed_transaction_count` makes
    id-deduplication visible to the caller. The remaining fields are
    `finance_analytics.data.quality.DataQualityReport` fields, reused as-is.
    """

    requested_transaction_count: int
    processed_transaction_count: int
    duplicate_row_count: int
    duplicate_id_count: int
    invalid_date_count: int
    invalid_amount_count: int
    insight_count: int
    anomaly_count: int
    recurring_count: int


class AnalysisResponse(BaseModel):
    """Response body for `POST /analytics/analyse` (PR-013 §6)."""

    summary: AnalysisSummary
    insights: list[InsightResponse]
    anomalies: list[AnomalyResponse]
    recurring: list[RecurringResponse]
    metadata: AnalysisMetadata


# --- Health -------------------------------------------------------------------


class HealthResponse(BaseModel):
    status: str = Field(description='Always "ok" when the service is reachable.')


# --- Errors ---------------------------------------------------------------


class ErrorResponse(BaseModel):
    """Structured error body. Never includes a stack trace or transaction
    payload (PR-013 §7, §16)."""

    detail: str
    errors: list[dict[str, Any]] | None = None
