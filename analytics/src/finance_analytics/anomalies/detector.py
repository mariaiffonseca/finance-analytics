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

import math
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
    """Round to display precision, or `None` for a value with nothing to
    show (`NaN`, from an empty history) or nothing sensible to show
    (`+inf`/`-inf` — not valid JSON and not orderable against other scores).
    """
    return None if not math.isfinite(value) else round(float(value), 2)


def _build_context(row: pd.Series) -> dict[str, object]:
    return {
        "merchant_median": _optional_round(row["merchant_median"]),
        "merchant_mad": _optional_round(row["merchant_mad"]),
        "merchant_transaction_count": int(row["merchant_history_count"]),
        "category_median": _optional_round(row["category_median"]),
        "category_mad": _optional_round(row["category_mad"]),
        "category_iqr": _optional_round(row["category_iqr"]),
        "category_transaction_count": int(row["category_history_count"]),
        "global_median": _optional_round(row["global_median"]),
        "global_mad": _optional_round(row["global_mad"]),
        "global_transaction_count": int(row["global_history_count"]),
    }


#: One entry per tier, in priority order: (method name, the row's history-count
#: field, the minimum required, the row's median/mad fields, the noun-phrase
#: getter — `None` for the noun-free global tier — and the `explain_*`
#: function for that tier). `_score_row` walks this instead of three
#: copy-pasted branches, so a fix to how a tier is scored can't be applied to
#: one branch and silently forgotten in the other two.
_TIERS = [
    (
        "merchant_relative_robust_z",
        "merchant_history_count",
        MIN_MERCHANT_HISTORY,
        "merchant_median",
        "merchant_mad",
        lambda row, merchant_column, category_column: row[merchant_column],
        explain_merchant_relative,
    ),
    (
        "category_relative_robust_z",
        "category_history_count",
        MIN_CATEGORY_HISTORY,
        "category_median",
        "category_mad",
        lambda row, merchant_column, category_column: row[category_column],
        explain_category_relative,
    ),
    (
        "global_relative_robust_z",
        "global_history_count",
        MIN_GLOBAL_HISTORY,
        "global_median",
        "global_mad",
        lambda row, merchant_column, category_column: None,
        explain_global_relative,
    ),
]


def _score_row(
    row: pd.Series,
    id_column: str,
    category_column: str,
    merchant_column: str,
    currency_column: str,
) -> AnomalyResult:
    amount = float(row["abs_amount"])
    currency = row[currency_column]
    context = _build_context(row)

    for method, count_key, min_history, median_key, mad_key, noun_of, explain in _TIERS:
        if row[count_key] < min_history:
            continue

        median, mad = row[median_key], row[mad_key]
        raw_score = robust_zscore_of(amount, median, mad)
        score = _optional_round(raw_score)
        # Deciding anomalousness from the *displayed* (rounded) score, not
        # the raw one, guarantees the two never disagree — a raw score of
        # 2.996 and one of 3.004 both round to the same displayed 3.0 and
        # must therefore get the same is_anomaly verdict.
        is_anomaly = score is not None and score > ANOMALY_Z_THRESHOLD
        is_unusually_low = score is not None and score < -ANOMALY_Z_THRESHOLD

        noun = noun_of(row, merchant_column, category_column)
        args = (amount, median, is_anomaly) if noun is None else (noun, amount, median, is_anomaly)
        reason = explain(*args, currency=currency, is_unusually_low=is_unusually_low)

        return AnomalyResult(
            transaction_id=row[id_column],
            anomaly_score=score,
            is_anomaly=is_anomaly,
            method=method,
            reason=reason,
            reference_context=context,
        )

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


def detect_anomalies(
    frame: pd.DataFrame,
    id_column: str = "id",
    date_column: str = "date",
    amount_column: str = "amount",
    category_column: str = "category",
    merchant_column: str = "merchant",
    currency_column: str = "currency",
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
        _score_row(row, id_column, category_column, merchant_column, currency_column)
        for _, row in features.iterrows()
    ]
