"""Recurring-transaction detection (PR-010).

**Method.** Fully deterministic, rule-based classification — no ML, no LLM.
For each merchant, `features.build_candidate_features` aggregates expense
transactions into occurrence count, amount statistics and interval
statistics; `scoring.combine_confidence` blends those into a single 0-1
confidence score; this module classifies each candidate into one of four
outcomes using explicit, documented thresholds:

1. **`"Insufficient history"`** — fewer than `MIN_OCCURRENCES_CANDIDATE`
   (2) occurrences. Nothing to compare against yet; not scored.
2. **`"Recurring"`** — at least `MIN_OCCURRENCES_FOR_RECURRING` (3)
   occurrences *and* `confidence_score >= RECURRING_CONFIDENCE_THRESHOLD`.
   The occurrence floor is structural, not tunable: with exactly 2
   occurrences there is exactly 1 interval, and interval *variation* needs
   at least 2 intervals to compute at all (see `features.py` and PR-008's
   EDA, notebook 02 section 8) — so a merchant can never be more than
   "Possible recurring" until a third occurrence lets interval consistency
   actually be assessed.
3. **`"Possible recurring"`** — at least `MIN_OCCURRENCES_CANDIDATE` (2)
   occurrences and `confidence_score >= POSSIBLE_RECURRING_CONFIDENCE_THRESHOLD`,
   but not enough to clear the "Recurring" bar (either the occurrence floor
   above, or a moderate confidence score).
4. **`"Not recurring"`** — at least `MIN_OCCURRENCES_CANDIDATE` occurrences
   but confidence below `POSSIBLE_RECURRING_CONFIDENCE_THRESHOLD`. This is
   the "frequent but irregular merchant" case PR-010 explicitly warns
   against auto-classifying as recurring: repeated merchant activity whose
   amount and/or interval are too inconsistent to look like a fixed charge.

**Why not ML.** PR-010's Critical Rule requires EDA to demonstrate
deterministic rules are insufficient before introducing ML. It does not:
PR-008's EDA (notebook 02, section 8) found a small, clean set of
recurring-candidate signals (occurrence count, amount variation, interval
and its variation) that already separate a clear case (`Spotify`: identical
amount, ~28-day gaps) from a clear non-case (`O Pescador`: ~52% amount
variation despite a similar gap) using simple, explainable arithmetic. The
fixture (22 rows, 18 clean, at most 2 occurrences per merchant) is also far
too small to train or validate a model against — the same reasoning
`anomalies/detector.py` used to reject `IsolationForest` for PR-009.

**Scope.** Expenses only, like `analysis.recurring` and `anomalies.*` — see
`features.py`'s docstring. Independent from PR-009: this module does not
import from or combine with `finance_analytics.anomalies` (PR-010,
"Relationship with Anomaly Detection" — that combination is explicitly
future work).
"""

from __future__ import annotations

import math
from dataclasses import dataclass, field

import pandas as pd

from finance_analytics.recurring.explanations import (
    explain_insufficient_history,
    explain_not_recurring,
    explain_possible_recurring,
    explain_recurring,
)
from finance_analytics.recurring.features import build_candidate_features
from finance_analytics.recurring.scoring import classify_frequency, combine_confidence

#: Fewer than this many occurrences and there is nothing to compare against
#: at all — mirrors `analysis.recurring.recurring_candidates`'s default
#: `min_occurrences` and `anomalies.detector.MIN_MERCHANT_HISTORY`.
MIN_OCCURRENCES_CANDIDATE = 2

#: Fewer than this many occurrences and interval *consistency* cannot be
#: assessed at all — see the module docstring. A candidate below this bar
#: is capped at "Possible recurring", never "Recurring".
MIN_OCCURRENCES_FOR_RECURRING = 3

#: Combined confidence score at/above which a candidate with enough history
#: to assess interval consistency is classified "Recurring".
RECURRING_CONFIDENCE_THRESHOLD = 0.70

#: Combined confidence score at/above which a candidate is classified
#: "Possible recurring" rather than "Not recurring".
POSSIBLE_RECURRING_CONFIDENCE_THRESHOLD = 0.45


@dataclass(frozen=True)
class RecurringResult:
    """Result of classifying a single merchant's expense history for
    recurrence.

    `is_recurring` is `True` only for `classification == "Recurring"` —
    `"Possible recurring"` is deliberately not collapsed into `True`
    (PR-010, Classification: three states, not two), so a consumer that
    only reads `is_recurring` gets the conservative answer, while one that
    reads `classification` gets the full picture.

    `confidence_score` is `None` when `classification == "Insufficient
    history"` — there was not enough history to score at all, distinct
    from "scored and found not recurring" (`confidence_score` present, low).
    """

    merchant: str
    is_recurring: bool
    classification: str
    confidence_score: float | None
    frequency: str
    occurrences: int
    median_amount: float | None
    amount_variation: float | None
    median_interval_days: float | None
    interval_variation: float | None
    first_seen: pd.Timestamp
    last_seen: pd.Timestamp
    reason: str
    transaction_ids: list[str] = field(default_factory=list)


def _optional(value: float, ndigits: int = 2) -> float | None:
    """Round to display precision, or `None` for a value with nothing to
    show (`NaN` — no history to compute it from)."""
    return None if not math.isfinite(value) else round(float(value), ndigits)


def _classify(row: pd.Series) -> tuple[str, float | None]:
    occurrences = int(row["occurrences"])
    if occurrences < MIN_OCCURRENCES_CANDIDATE:
        return "Insufficient history", None

    confidence = combine_confidence(
        amount_variation=row["amount_variation"],
        interval_variation=row["interval_variation"],
        occurrences=occurrences,
        first_seen=row["first_seen"],
        last_seen=row["last_seen"],
        median_interval_days=row["median_interval_days"],
    )

    if (
        occurrences >= MIN_OCCURRENCES_FOR_RECURRING
        and confidence >= RECURRING_CONFIDENCE_THRESHOLD
    ):
        return "Recurring", confidence
    if confidence >= POSSIBLE_RECURRING_CONFIDENCE_THRESHOLD:
        return "Possible recurring", confidence
    return "Not recurring", confidence


def _explain(classification: str, row: pd.Series) -> str:
    merchant = row["merchant"]
    occurrences = int(row["occurrences"])

    if classification == "Insufficient history":
        return explain_insufficient_history(merchant, occurrences, MIN_OCCURRENCES_CANDIDATE)
    if classification == "Recurring":
        frequency = classify_frequency(row["median_interval_days"])
        return explain_recurring(
            merchant,
            occurrences,
            row["median_interval_days"],
            frequency,
            row["median_amount"],
            row["currency"],
            row["amount_variation"],
        )
    if classification == "Possible recurring":
        return explain_possible_recurring(
            merchant,
            occurrences,
            row["median_interval_days"],
            row["median_amount"],
            row["currency"],
            MIN_OCCURRENCES_FOR_RECURRING,
        )
    return explain_not_recurring(
        merchant, occurrences, row["amount_variation"], row["interval_variation"]
    )


def detect_recurring_transactions(
    frame: pd.DataFrame,
    id_column: str = "id",
    date_column: str = "date",
    amount_column: str = "amount",
    merchant_column: str = "merchant",
    currency_column: str = "currency",
) -> list[RecurringResult]:
    """Classify every merchant's expense history in `frame` for recurrence.

    Expects already-validated, deduplicated input, sorted or not (rows are
    re-sorted chronologically internally per merchant, see `features.py`).
    `Income` rows are excluded — see `features.py`'s docstring. Deterministic:
    the same input always produces the same list of results in the same
    (alphabetical-by-merchant) order.
    """
    candidates = build_candidate_features(
        frame,
        id_column=id_column,
        date_column=date_column,
        amount_column=amount_column,
        merchant_column=merchant_column,
        currency_column=currency_column,
    )

    results = []
    for _, row in candidates.iterrows():
        classification, confidence = _classify(row)
        frequency = (
            classify_frequency(row["median_interval_days"])
            if classification != "Insufficient history"
            else "Unknown"
        )
        reason = _explain(classification, row)

        results.append(
            RecurringResult(
                merchant=row["merchant"],
                is_recurring=classification == "Recurring",
                classification=classification,
                confidence_score=confidence,
                frequency=frequency,
                occurrences=int(row["occurrences"]),
                median_amount=_optional(row["median_amount"]),
                amount_variation=_optional(row["amount_variation"], ndigits=4),
                median_interval_days=_optional(row["median_interval_days"], ndigits=1),
                interval_variation=_optional(row["interval_variation"], ndigits=4),
                first_seen=row["first_seen"],
                last_seen=row["last_seen"],
                reason=reason,
                transaction_ids=list(row["transaction_ids"]),
            )
        )

    return results
