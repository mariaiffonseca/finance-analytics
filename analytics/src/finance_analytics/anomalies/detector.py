"""Transaction anomaly detection (PR-009).

**Method.** A historical, leakage-free robust z-score (median/MAD), scored
against the most specific baseline with enough history to trust, in this
order:

1. **Merchant-relative** — this merchant's own prior amounts.
2. **Category-relative** — this category's prior amounts.
3. **Global** — every prior expense, regardless of merchant/category.
4. **`insufficient_history`** — none of the above has enough data; the
   transaction is not scored.

**Why this method, and why not others.** PR-008's EDA is the evidence base
(see `notebooks/03_anomaly_detection.ipynb` for the full comparison):

- A *global* robust z-score on expense magnitude cleanly separated the two
  genuine standout purchases (MediaMarkt, TAP Air) from everything else in
  the EDA fixture — so robust z-score is the scoring primitive used at every
  tier here, not just globally.
- A *global* z-score on signed amount (not expense-only) misclassified
  salary as an outlier — this module only ever scores expenses.
- *Category-relative* IQR returned nothing on the EDA fixture: most
  categories there have 1-3 transactions, too few to define "usual" at all.
  That is a sample-size problem, not evidence the approach is wrong — so
  it is kept, but gated behind an explicit minimum-history threshold rather
  than trusted unconditionally.
- *Merchant-relative* was not evaluated as a production method in the EDA,
  but its recurring-transaction investigation showed some merchants
  (`Spotify`) charge an identical amount every time even with only 2
  observations — a tight, trustworthy baseline that a `MIN_CATEGORY_HISTORY`
  gate tuned for noisier category data would needlessly discard. Hence its
  own, lower threshold.
- `sklearn.ensemble.IsolationForest` is not used: the EDA fixture has 18
  clean rows. There is no volume here to justify — or even meaningfully
  validate — an ML model, and PR-009 explicitly warns against using ML
  merely because scikit-learn is available.

**Historical context.** Every reference statistic comes from
`features.build_historical_features`, which only looks at transactions
strictly before the one being scored (chronological order) — a transaction
never contributes to its own baseline.

**Scope.** Expenses only, like the rest of `analysis/*`. `Income` rows are
not evaluated.
"""

from __future__ import annotations

from dataclasses import dataclass, field

import pandas as pd

from finance_analytics.analysis.outliers import robust_zscore_of
from finance_analytics.anomalies.explanations import (
    explain_category_relative,
    explain_global_relative,
    explain_insufficient_history,
    explain_merchant_relative,
)
from finance_analytics.anomalies.features import build_historical_features

#: Minimum prior same-merchant transactions before a merchant baseline is
#: trusted. Low relative to the category/global thresholds: a merchant
#: history is about *that merchant's own* consistency (e.g. an identical
#: subscription charge), which two observations can already establish —
#: see `recurring.recurring_candidates`'s use of the same minimum.
MIN_MERCHANT_HISTORY = 2

#: Minimum prior same-category transactions before a category baseline is
#: trusted. Higher than the merchant threshold: category spend mixes
#: multiple merchants and purchase types, so it needs more points before a
#: median/MAD stops being dominated by a single observation.
MIN_CATEGORY_HISTORY = 3

#: Minimum prior expense transactions overall before the global baseline —
#: the last resort when merchant and category history are both too thin —
#: is trusted.
MIN_GLOBAL_HISTORY = 5

#: A transaction scores as anomalous when its robust z-score exceeds this
#: many MAD-scaled deviations above the reference median. Only the positive
#: direction is flagged: an unusually *large* transaction is what this
#: product cares about (PR-009's own explainability example, "3.8× higher
#: than typical"); an unusually *small* one has no analogous product need
#: and is not flagged. 3.0 mirrors the separation PR-008's EDA observed
#: between genuine standouts (z ~17-24) and everything else (z ~1-2).
ANOMALY_Z_THRESHOLD = 3.0


@dataclass(frozen=True)
class AnomalyResult:
    """Result of scoring a single transaction for anomalousness.

    `anomaly_score` and `is_anomaly` are `None`/`False` when `method` is
    `"insufficient_history"` — there was not enough history to score the
    transaction against anything, which is a distinct outcome from "scored
    and found normal".
    """

    transaction_id: str
    anomaly_score: float | None
    is_anomaly: bool
    method: str
    reason: str
    reference_context: dict[str, object] = field(default_factory=dict)


def _optional_round(value: float) -> float | None:
    return None if pd.isna(value) else round(float(value), 2)


def _build_context(row: pd.Series) -> dict[str, object]:
    return {
        "merchant_median": _optional_round(row["merchant_median"]),
        "merchant_transaction_count": int(row["merchant_history_count"]),
        "category_median": _optional_round(row["category_median"]),
        "category_iqr": _optional_round(row["category_iqr"]),
        "category_transaction_count": int(row["category_history_count"]),
        "global_transaction_count": int(row["global_history_count"]),
    }


def _score_row(
    row: pd.Series, id_column: str, category_column: str, merchant_column: str
) -> AnomalyResult:
    amount = float(row["abs_amount"])
    category = row[category_column]
    merchant = row[merchant_column]
    context = _build_context(row)

    if row["merchant_history_count"] >= MIN_MERCHANT_HISTORY:
        method = "merchant_relative_robust_z"
        median, mad = row["merchant_median"], row["merchant_mad"]
        score = robust_zscore_of(amount, median, mad)
        is_anomaly = score > ANOMALY_Z_THRESHOLD
        reason = explain_merchant_relative(merchant, amount, median, is_anomaly)
    elif row["category_history_count"] >= MIN_CATEGORY_HISTORY:
        method = "category_relative_robust_z"
        median, mad = row["category_median"], row["category_mad"]
        score = robust_zscore_of(amount, median, mad)
        is_anomaly = score > ANOMALY_Z_THRESHOLD
        reason = explain_category_relative(category, amount, median, is_anomaly)
    elif row["global_history_count"] >= MIN_GLOBAL_HISTORY:
        method = "global_relative_robust_z"
        median, mad = row["global_median"], row["global_mad"]
        score = robust_zscore_of(amount, median, mad)
        is_anomaly = score > ANOMALY_Z_THRESHOLD
        reason = explain_global_relative(amount, median, is_anomaly)
    else:
        return AnomalyResult(
            transaction_id=row[id_column],
            anomaly_score=None,
            is_anomaly=False,
            method="insufficient_history",
            reason=explain_insufficient_history(
                category_count=int(row["category_history_count"]),
                merchant_count=int(row["merchant_history_count"]),
                global_count=int(row["global_history_count"]),
            ),
            reference_context=context,
        )

    return AnomalyResult(
        transaction_id=row[id_column],
        anomaly_score=_optional_round(score),
        is_anomaly=bool(is_anomaly),
        method=method,
        reason=reason,
        reference_context=context,
    )


def detect_anomalies(
    frame: pd.DataFrame,
    id_column: str = "id",
    date_column: str = "date",
    amount_column: str = "amount",
    category_column: str = "category",
    merchant_column: str = "merchant",
) -> list[AnomalyResult]:
    """Score every expense transaction in `frame` for anomalousness.

    Expects already-validated, deduplicated input, sorted or not — rows are
    re-sorted chronologically internally (see `features.build_historical_features`).
    `Income` rows are excluded, not scored. Deterministic: the same input
    always produces the same list of results in the same (chronological)
    order.
    """
    features = build_historical_features(
        frame,
        date_column=date_column,
        amount_column=amount_column,
        category_column=category_column,
        merchant_column=merchant_column,
    )

    return [
        _score_row(row, id_column, category_column, merchant_column)
        for _, row in features.iterrows()
    ]
