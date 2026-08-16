"""Historical, leakage-free feature engineering for anomaly detection.

Every statistic here is computed from transactions strictly before the one
being scored (PR-009, Historical Context) — a transaction never contributes
to its own baseline. Scoped to expenses only, consistent with the rest of
`analysis/*` (see `analysis/category.py`, `analysis/merchant.py`): anomaly
detection asks "is this spend unusual", and `Income` is a different kind of
thing (PR-008's EDA found a naive detector that didn't make this
distinction misclassified salary as an outlier).

Three reference baselines are tracked per transaction — merchant, category
and global — each with its own history count, median and MAD (median
absolute deviation). `detector.py` picks which baseline is trustworthy
enough to score against; this module only computes the raw ingredients, so
each can be tested independently of that decision.
"""

from __future__ import annotations

import bisect
from collections import defaultdict

import pandas as pd

from finance_analytics.analysis.outliers import percentile_of_sorted


def _median_mad_iqr(sorted_history: list[float]) -> tuple[int, float, float, float]:
    """Summarise an already-sorted list of historical amounts as
    (count, median, mad, iqr).

    `sorted_history` is expected pre-sorted (see `build_historical_features`,
    which maintains each running history via `bisect.insort`) so median and
    IQR are O(1) lookups via `percentile_of_sorted` instead of re-sorting
    the full history from scratch on every transaction. Only MAD's
    deviations-from-median need their own sort, since they change whenever
    the median does.

    All three statistics are `NaN` for an empty history — there is nothing
    to summarise yet, and `NaN` (rather than e.g. 0) makes that explicit to
    callers instead of looking like a real, if degenerate, baseline.
    """
    n = len(sorted_history)
    if n == 0:
        return 0, float("nan"), float("nan"), float("nan")

    median = percentile_of_sorted(sorted_history, 0.5)
    deviations = sorted(abs(v - median) for v in sorted_history)
    mad = percentile_of_sorted(deviations, 0.5)
    q1 = percentile_of_sorted(sorted_history, 0.25)
    q3 = percentile_of_sorted(sorted_history, 0.75)
    return n, median, mad, q3 - q1


def build_historical_features(
    frame: pd.DataFrame,
    date_column: str = "date",
    amount_column: str = "amount",
    category_column: str = "category",
    merchant_column: str = "merchant",
) -> pd.DataFrame:
    """Return expense rows of `frame` with historical reference features.

    Expects already-validated, deduplicated input (the same convention as
    `analysis.category.category_summary` / `analysis.merchant.merchant_summary`)
    — rows with a missing date, amount, merchant or category are dropped
    rather than silently scored against an incomplete or bogus history (a
    missing category would otherwise pool into a shared "NaN category"
    baseline with every other uncategorised row).

    Rows are processed in chronological order (`date_column`, stable sort so
    same-day transactions keep their original relative order). For each row,
    `merchant_history_count`/`_median`/`_mad`, `category_history_count`/...
    and `global_history_count`/... are computed from every prior row's
    `abs_amount` sharing that merchant/category/anything respectively — the
    current row is appended to each running history only *after* its
    features are computed, so it can never influence its own baseline.
    `*_iqr` (category and global) is included for reference/display
    alongside the median, mirroring PR-009's suggested result fields.

    Each running history is maintained pre-sorted (`bisect.insort`) so
    `_median_mad_iqr` can look up the median and IQR in O(1) instead of
    resorting the full history from scratch on every transaction.
    """
    expenses = (
        frame[frame[amount_column] < 0]
        .dropna(subset=[date_column, amount_column, merchant_column, category_column])
        .sort_values(date_column, kind="stable")
        .copy()
    )
    expenses["abs_amount"] = expenses[amount_column].abs()

    merchant_history: dict[object, list[float]] = defaultdict(list)
    category_history: dict[object, list[float]] = defaultdict(list)
    global_history: list[float] = []

    feature_rows = []
    for _, row in expenses.iterrows():
        merchant = row[merchant_column]
        category = row[category_column]
        amount = float(row["abs_amount"])

        m_count, m_median, m_mad, _m_iqr = _median_mad_iqr(merchant_history[merchant])
        c_count, c_median, c_mad, c_iqr = _median_mad_iqr(category_history[category])
        g_count, g_median, g_mad, g_iqr = _median_mad_iqr(global_history)

        feature_rows.append(
            {
                "merchant_history_count": m_count,
                "merchant_median": m_median,
                "merchant_mad": m_mad,
                "category_history_count": c_count,
                "category_median": c_median,
                "category_mad": c_mad,
                "category_iqr": c_iqr,
                "global_history_count": g_count,
                "global_median": g_median,
                "global_mad": g_mad,
                "global_iqr": g_iqr,
            }
        )

        # Update *after* reading, so this row never contributes to its own
        # baseline. `bisect.insort` keeps each history sorted in place.
        bisect.insort(merchant_history[merchant], amount)
        bisect.insort(category_history[category], amount)
        bisect.insort(global_history, amount)

    features = pd.DataFrame(feature_rows, index=expenses.index)
    return pd.concat([expenses, features], axis=1)
