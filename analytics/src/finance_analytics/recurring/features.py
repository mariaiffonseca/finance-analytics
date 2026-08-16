"""Per-merchant candidate feature engineering for recurring-transaction
detection (PR-010).

Builds the full candidate table used by `detector.detect_recurring_transactions`:
one row per merchant with at least one expense transaction. Unlike
`analysis.recurring.recurring_candidates` (PR-008's *exploratory* table),
merchants seen only once are kept rather than filtered out — with
`occurrences=1` and `NaN` variation stats — so `detector.py` can classify
them explicitly as insufficient history, the same "visible not-enough-data"
outcome `anomalies.detector` uses for its `insufficient_history` tier,
rather than silently dropping them from the result set.

**Scope: expenses only.** A "recurring transaction" here means a recurring
*payment* — a subscription or regular bill. Income repeats too (e.g. a
monthly salary), but PR-010 explicitly scopes detection to expenses unless
product requirements justify otherwise, and the fixture has only one
salary row and one freelance-payment row — not enough evidence to justify
widening scope. This mirrors `analysis/recurring.py` and
`anomalies/features.py`, which make the same choice.

**No merchant-name normalisation.** PR-010's Merchant Normalisation section
says to introduce only the minimum deterministic normalisation the EDA
shows is needed. The fixture's merchant names have no case, whitespace or
formatting variants to normalise, so none is applied here — exact string
match, same as `analysis/recurring.py` and `anomalies/features.py`. If a
future dataset needs normalisation, that decision belongs in this
docstring and in a dedicated, tested function — not silently inside the
grouping logic.
"""

from __future__ import annotations

import pandas as pd

_COLUMNS = [
    "merchant",
    "currency",
    "occurrences",
    "total_amount",
    "median_amount",
    "amount_variation",
    "median_interval_days",
    "interval_variation",
    "first_seen",
    "last_seen",
    "transaction_ids",
]


def build_candidate_features(
    frame: pd.DataFrame,
    id_column: str = "id",
    date_column: str = "date",
    amount_column: str = "amount",
    merchant_column: str = "merchant",
    currency_column: str = "currency",
) -> pd.DataFrame:
    """Aggregate expense transactions into one candidate row per merchant.

    Expects already-validated, deduplicated input (the same convention as
    `analysis.category.category_summary` and `anomalies.features.build_historical_features`)
    — rows with a missing date, amount or merchant are dropped rather than
    silently aggregated into a bogus candidate.

    Columns, all derived from that merchant's own expense transactions:

    - `occurrences` — transaction count. A merchant with exactly 1 has
      nothing to compare against yet; `detector.py` treats that as
      insufficient history rather than "not recurring".
    - `total_amount`, `median_amount` — expense magnitude (positive).
    - `amount_variation` — coefficient of variation (std / mean) of
      transaction amounts. Low means a stable charge; `NaN` only when the
      mean is 0 (a currently-impossible edge case for expense magnitudes,
      guarded rather than left to divide-by-zero).
    - `median_interval_days`, `interval_variation` — robust statistics on
      the day-gaps between consecutive transactions (chronological order).
      `interval_variation` is `NaN` whenever fewer than 2 intervals exist
      (i.e. `occurrences < 3`) — a single gap has nothing to compute
      *variation* against. This is a structural limit of the data, not a
      bug (PR-008's EDA, notebook 02 section 8, reaches the same
      conclusion for the same reason).
    - `first_seen`, `last_seen` — date range this merchant was observed
      over.
    - `transaction_ids` — the contributing transaction IDs, so a future
      caller (e.g. an Insights layer) can link a classification back to
      specific transactions without re-deriving the grouping.

    Returned rows are sorted by `merchant` for a deterministic, easy-to-diff
    order. An input with no expense rows returns an empty frame with the
    documented columns.
    """
    expenses = frame[frame[amount_column] < 0].dropna(
        subset=[date_column, amount_column, merchant_column]
    )

    rows = []
    for merchant, group in expenses.groupby(merchant_column):
        ordered = group.sort_values(date_column, kind="stable")
        amounts = ordered[amount_column].abs()
        dates = ordered[date_column]

        amount_mean = amounts.mean()
        amount_variation = float(amounts.std() / amount_mean) if amount_mean else float("nan")

        intervals = dates.diff().dropna().dt.days
        median_interval_days = float(intervals.median()) if not intervals.empty else float("nan")
        interval_mean = intervals.mean()
        interval_variation = (
            float(intervals.std() / interval_mean) if interval_mean else float("nan")
        )

        rows.append(
            {
                "merchant": merchant,
                "currency": ordered[currency_column].iloc[0],
                "occurrences": len(ordered),
                "total_amount": float(amounts.sum()),
                "median_amount": float(amounts.median()),
                "amount_variation": amount_variation,
                "median_interval_days": median_interval_days,
                "interval_variation": interval_variation,
                "first_seen": dates.min(),
                "last_seen": dates.max(),
                "transaction_ids": list(ordered[id_column]),
            }
        )

    if not rows:
        return pd.DataFrame(columns=_COLUMNS)

    return pd.DataFrame(rows, columns=_COLUMNS).sort_values("merchant").reset_index(drop=True)
