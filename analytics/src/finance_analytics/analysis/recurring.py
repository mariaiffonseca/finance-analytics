"""Recurring-transaction candidate exploration.

Builds an exploratory candidate table for assessing whether recurring
detection is feasible (PR-008, RQ5) — not the final recurring-payment
detector.
"""

from __future__ import annotations

import pandas as pd

_COLUMNS = [
    "merchant",
    "occurrences",
    "amount_variation",
    "median_interval_days",
    "interval_variation",
]


def recurring_candidates(
    frame: pd.DataFrame,
    date_column: str = "date",
    amount_column: str = "amount",
    merchant_column: str = "merchant",
    min_occurrences: int = 2,
) -> pd.DataFrame:
    """Build a candidate table of merchants with repeated transactions.

    Columns: `merchant`, `occurrences`, `amount_variation`,
    `median_interval_days`, `interval_variation`. `amount_variation` and
    `interval_variation` are coefficients of variation (std / mean) of the
    transaction amounts and of the day gaps between consecutive
    transactions — a low value suggests a stable amount/cadence worth
    investigating further, not a confirmed subscription. Merchants with
    fewer than `min_occurrences` transactions (nothing to compare) are
    excluded.
    """
    working = frame.dropna(subset=[date_column, amount_column, merchant_column]).sort_values(
        date_column
    )

    rows = []
    for merchant, group in working.groupby(merchant_column):
        if len(group) < min_occurrences:
            continue

        amounts = group[amount_column]
        amount_mean = amounts.abs().mean()
        amount_variation = float(amounts.std() / amount_mean) if amount_mean else float("nan")

        intervals = group[date_column].diff().dropna().dt.days
        interval_mean = intervals.mean()
        median_interval_days = float(intervals.median()) if not intervals.empty else float("nan")
        # With exactly 2 occurrences there's a single gap, so std() (and thus
        # interval_variation) is NaN by construction, not a bug — see the
        # notebook's Feature Engineering Candidates section.
        interval_variation = (
            float(intervals.std() / interval_mean) if interval_mean else float("nan")
        )

        rows.append(
            {
                "merchant": merchant,
                "occurrences": len(group),
                "amount_variation": amount_variation,
                "median_interval_days": median_interval_days,
                "interval_variation": interval_variation,
            }
        )

    if not rows:
        return pd.DataFrame(columns=_COLUMNS)

    return (
        pd.DataFrame(rows, columns=_COLUMNS)
        .sort_values("occurrences", ascending=False)
        .reset_index(drop=True)
    )
