"""Adapts PR-009 anomaly results and PR-010 recurring results into
`Insight` objects (PR-012 §2 — Unusual Transaction, Recurring Payment).

Neither anomaly detection nor recurring-transaction classification is
reimplemented here. Every score, threshold and explanation this module uses
comes directly from `finance_analytics.anomalies.detector.AnomalyResult` /
`finance_analytics.recurring.detector.RecurringResult` — this module only
decides which results are insight-worthy and repackages them into the
structured `Insight` model (PR-012, Critical Principle: "Never invent
facts").
"""

from __future__ import annotations

import pandas as pd

from finance_analytics.anomalies.detector import AnomalyResult
from finance_analytics.insights.models import (
    IMPORTANT,
    INFO,
    NOTICE,
    RECURRING_PAYMENT,
    UNUSUAL_TRANSACTION,
    Insight,
)
from finance_analytics.recurring.detector import RecurringResult

#: Confidence assigned per anomaly-detection tier, mirroring
#: `enrichment.categories.CONFIDENCE_BY_METHOD`'s convention of a fixed
#: score per method rather than a derived one: a merchant-relative baseline
#: is the most specific evidence available (this merchant's own history),
#: category-relative is broader, global-relative is the least specific
#: fallback — see `anomalies.detector`'s own tier-ordering rationale. Not a
#: calibrated probability.
ANOMALY_METHOD_CONFIDENCE: dict[str, float] = {
    "merchant_relative_robust_z": 0.9,
    "category_relative_robust_z": 0.75,
    "global_relative_robust_z": 0.6,
}

#: z-score at/above which an unusual-transaction insight is escalated from
#: NOTICE to IMPORTANT. Double `anomalies.detector.ANOMALY_Z_THRESHOLD`
#: (3.0) — the same separation PR-009's own threshold was justified by
#: (PR-008's EDA: genuine standouts at z ~17-24 vs noise at z ~1-2) still
#: holds at double the flagging bar, so this is not an independently chosen
#: number.
ANOMALY_IMPORTANT_Z_THRESHOLD = 6.0

#: Recurring-transaction insight severity per `RecurringResult.classification`.
#: `"Not recurring"` and `"Insufficient history"` are absent on purpose —
#: neither produces an insight (see `recurring_payment_insights`).
_RECURRING_SEVERITY: dict[str, str] = {
    "Recurring": NOTICE,
    "Possible recurring": INFO,
}


def _clean(value: object) -> str | None:
    return None if pd.isna(value) else str(value)


def unusual_transaction_insights(
    anomaly_results: list[AnomalyResult],
    frame: pd.DataFrame,
    id_column: str = "id",
    category_column: str = "category",
    merchant_column: str = "merchant",
    amount_column: str = "amount",
) -> list[Insight]:
    """One `Insight` per transaction `detect_anomalies` flagged
    `is_anomaly=True`.

    Transactions that were not flagged, or had insufficient history to be
    scored at all, produce no insight: an unflagged transaction is not
    noteworthy, and "insufficient history" is a data-coverage fact, not a
    behavioural finding. `frame` supplies the merchant/category/amount
    context `AnomalyResult` itself does not carry (only aggregate
    reference statistics) — it must be the same already-deduplicated frame
    `detect_anomalies` was run against, so transaction ids line up.
    """
    if not anomaly_results:
        return []

    lookup = frame.set_index(frame[id_column].astype(str))

    insights = []
    for result in anomaly_results:
        if not result.is_anomaly:
            continue
        tx_id = str(result.transaction_id)
        if tx_id not in lookup.index:
            continue
        row = lookup.loc[tx_id]

        confidence = ANOMALY_METHOD_CONFIDENCE.get(result.method, 0.5)
        score = result.anomaly_score or 0.0
        severity = IMPORTANT if score >= ANOMALY_IMPORTANT_Z_THRESHOLD else NOTICE
        amount = row.get(amount_column)

        insights.append(
            Insight(
                id=f"unusual_transaction:{tx_id}",
                type=UNUSUAL_TRANSACTION,
                title="Unusual transaction",
                description=result.reason,
                severity=severity,
                confidence=confidence,
                related_transaction_ids=(tx_id,),
                category=_clean(row.get(category_column)),
                merchant=_clean(row.get(merchant_column)),
                amount=None if pd.isna(amount) else round(float(amount), 2),
                metadata={
                    "anomaly_score": result.anomaly_score,
                    "method": result.method,
                    "reference_context": dict(result.reference_context),
                    "source_feature": "anomalies.detect_anomalies",
                },
            )
        )
    return insights


def recurring_payment_insights(recurring_results: list[RecurringResult]) -> list[Insight]:
    """One `Insight` per merchant classified `"Recurring"` or `"Possible
    recurring"` by `detect_recurring_transactions`.

    `"Not recurring"` and `"Insufficient history"` merchants produce no
    insight — neither is a finding worth surfacing to a user.
    `confidence` is `RecurringResult.confidence_score` itself, not
    recomputed here.
    """
    insights = []
    for result in recurring_results:
        severity = _RECURRING_SEVERITY.get(result.classification)
        if severity is None:
            continue

        insights.append(
            Insight(
                id=f"recurring_payment:{result.merchant}:{result.currency}",
                type=RECURRING_PAYMENT,
                title=f"{result.classification}: {result.merchant}",
                description=result.reason,
                severity=severity,
                confidence=result.confidence_score or 0.0,
                related_transaction_ids=tuple(str(t) for t in result.transaction_ids),
                merchant=result.merchant,
                amount=result.median_amount,
                metadata={
                    "classification": result.classification,
                    "frequency": result.frequency,
                    "occurrences": result.occurrences,
                    "median_interval_days": result.median_interval_days,
                    "amount_variation": result.amount_variation,
                    "first_seen": (
                        str(result.first_seen.date()) if pd.notna(result.first_seen) else None
                    ),
                    "last_seen": (
                        str(result.last_seen.date()) if pd.notna(result.last_seen) else None
                    ),
                    "source_feature": "recurring.detect_recurring_transactions",
                },
            )
        )
    return insights
