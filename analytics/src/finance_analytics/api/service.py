"""Application/service layer between the API transport and the analytics
engine (PR-013 §8, Critical Architecture Rule):

```
API Layer            (routes/*.py — HTTP concerns only)
    v
Application Layer    (this module — orchestration + type conversion)
    v
Analytics Engine      (enrichment / anomalies / recurring / insights)
```

`run_analysis` is the only function this module exposes to routes. It:

1. Converts `AnalysisRequest.transactions` into the `pandas.DataFrame`
   shape the analytics engine expects (`_transactions_to_frame` — the
   JSON-payload analogue of `finance_analytics.io.csv.load_transactions_csv`:
   loading only, no analysis).
2. Reuses `finance_analytics.validation.transactions.validate_transactions`
   (PR-007) as a second, defense-in-depth structural check beyond Pydantic's
   own schema validation — e.g. a JSON body can encode `amount: NaN`, which
   Pydantic's `float` field accepts but is meaningless for analytics.
3. Calls the existing, unmodified analytics functions —
   `detect_anomalies` (PR-009), `detect_recurring_transactions` (PR-010),
   `generate_insights` (PR-012, itself calling PR-011's enrichment) — and
   two small PR-007 building blocks for the financial summary
   (`duckdb_queries.total_income`/`total_expenses`,
   `data.quality.build_quality_report`).
4. Maps the results onto the API's own response schemas.

No anomaly/recurring threshold, insight rule, or feature-engineering step
is implemented here — every analytical decision happens inside the modules
this function calls.
"""

from __future__ import annotations

import pandas as pd

from finance_analytics.anomalies.detector import AnomalyResult, detect_anomalies
from finance_analytics.api.schemas import (
    AnalysisMetadata,
    AnalysisRequest,
    AnalysisResponse,
    AnalysisSummary,
    AnomalyResponse,
    InsightResponse,
    RecurringResponse,
    TransactionRequest,
)
from finance_analytics.data.quality import DataQualityReport, build_quality_report
from finance_analytics.data.schema import REQUIRED_COLUMNS
from finance_analytics.duckdb_queries import connect_with_transactions, total_expenses, total_income
from finance_analytics.insights.engine import generate_insights
from finance_analytics.insights.models import Insight
from finance_analytics.recurring.detector import RecurringResult, detect_recurring_transactions
from finance_analytics.validation.transactions import ValidationResult, validate_transactions

#: `RecurringResult.classification` values worth returning to a caller —
#: mirrors `insights.conversions.recurring_payment_insights`'s filter.
_REPORTABLE_RECURRING_CLASSIFICATIONS = {"Recurring", "Possible recurring"}


class TransactionValidationError(Exception):
    """Raised when `validate_transactions` finds a structural or row-level
    problem Pydantic's schema validation did not catch. Mapped to HTTP 400
    by `api.errors` — distinct from Pydantic's own 422 for malformed JSON
    types/shapes."""

    def __init__(self, result: ValidationResult) -> None:
        self.result = result
        super().__init__(_describe(result))


def _describe(result: ValidationResult) -> str:
    if result.missing_columns:
        return f"Missing required columns: {', '.join(result.missing_columns)}."
    if result.is_empty:
        return "Transaction dataset is empty."
    if result.invalid_date_rows:
        return f"Invalid date on row(s): {list(result.invalid_date_rows)}."
    if result.invalid_amount_rows:
        return f"Invalid amount on row(s): {list(result.invalid_amount_rows)}."
    columns = ", ".join(result.missing_value_rows)
    return f"Missing required value(s) in column(s): {columns}."


def _transactions_to_frame(transactions: list[TransactionRequest]) -> pd.DataFrame:
    """Build the DataFrame the analytics engine expects from validated
    request models. Structural loading only, matching `io.csv`'s own
    separation of loading from validation/analysis."""
    records = [transaction.model_dump(mode="json") for transaction in transactions]
    frame = pd.DataFrame.from_records(records, columns=list(REQUIRED_COLUMNS))
    frame["date"] = pd.to_datetime(frame["date"], format="%Y-%m-%d", errors="coerce")
    frame["amount"] = pd.to_numeric(frame["amount"], errors="coerce")
    return frame


def _insight_to_schema(insight: Insight) -> InsightResponse:
    return InsightResponse(**insight.to_dict())


def _anomaly_to_schema(result: AnomalyResult) -> AnomalyResponse:
    return AnomalyResponse(
        transaction_id=result.transaction_id,
        anomaly_score=result.anomaly_score,
        is_anomaly=result.is_anomaly,
        method=result.method,
        reason=result.reason,
        reference_context=dict(result.reference_context),
    )


def _recurring_to_schema(result: RecurringResult) -> RecurringResponse:
    return RecurringResponse(
        merchant=result.merchant,
        currency=result.currency,
        is_recurring=result.is_recurring,
        classification=result.classification,
        confidence_score=result.confidence_score,
        frequency=result.frequency,
        occurrences=result.occurrences,
        median_amount=result.median_amount,
        amount_variation=result.amount_variation,
        median_interval_days=result.median_interval_days,
        interval_variation=result.interval_variation,
        first_seen=result.first_seen.date(),
        last_seen=result.last_seen.date(),
        reason=result.reason,
        transaction_ids=list(result.transaction_ids),
    )


def _build_summary(deduplicated: pd.DataFrame, quality: DataQualityReport) -> AnalysisSummary:
    connection = connect_with_transactions(deduplicated)
    date_start, date_end = (None, None)
    if quality.date_range is not None:
        date_start, date_end = (ts.date() for ts in quality.date_range)

    income = total_income(connection)
    expenses = total_expenses(connection)
    return AnalysisSummary(
        transaction_count=quality.row_count,
        income_count=quality.income_count,
        expense_count=quality.expense_count,
        total_income=round(income, 2),
        total_expenses=round(expenses, 2),
        net_savings=round(income + expenses, 2),
        date_range_start=date_start,
        date_range_end=date_end,
        unique_merchant_count=quality.unique_merchant_count,
        unique_category_count=quality.unique_category_count,
    )


def run_analysis(request: AnalysisRequest) -> AnalysisResponse:
    """Orchestrate the full pipeline for one `POST /analytics/analyse` call
    (PR-013 §8): transactions -> enrichment -> analytics -> insights ->
    response. Stateless — nothing is persisted or cached across calls."""
    frame = _transactions_to_frame(request.transactions)

    structural_check = validate_transactions(frame)
    if not structural_check.is_valid:
        raise TransactionValidationError(structural_check)

    deduplicated = frame.drop_duplicates(subset=["id"]).copy()
    deduplicated["id"] = deduplicated["id"].astype(str)

    insights = generate_insights(deduplicated)
    anomalies = [result for result in detect_anomalies(deduplicated) if result.is_anomaly]
    recurring = [
        result
        for result in detect_recurring_transactions(deduplicated)
        if result.classification in _REPORTABLE_RECURRING_CLASSIFICATIONS
    ]

    quality = build_quality_report(deduplicated)

    return AnalysisResponse(
        summary=_build_summary(deduplicated, quality),
        insights=[_insight_to_schema(insight) for insight in insights],
        anomalies=[_anomaly_to_schema(result) for result in anomalies],
        recurring=[_recurring_to_schema(result) for result in recurring],
        metadata=AnalysisMetadata(
            requested_transaction_count=len(request.transactions),
            processed_transaction_count=len(deduplicated),
            duplicate_row_count=quality.duplicate_row_count,
            duplicate_id_count=quality.duplicate_id_count,
            invalid_date_count=quality.invalid_date_count,
            invalid_amount_count=quality.invalid_amount_count,
            insight_count=len(insights),
            anomaly_count=len(anomalies),
            recurring_count=len(recurring),
        ),
    )
