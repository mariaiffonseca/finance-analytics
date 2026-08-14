"""Outlier-investigation heuristics for exploratory analysis.

Analytical building blocks for investigating unusual transactions
(PR-008, RQ6) — not a production anomaly detector. A value flagged here is
a candidate worth a closer look, not a confirmed anomaly: a large
transaction is not automatically unusual for its category.
"""

from __future__ import annotations

import pandas as pd

#: Standard constant that makes MAD a consistent estimator of the standard
#: deviation under normality, so robust z-scores sit on a roughly
#: comparable scale to ordinary z-scores.
_MAD_TO_STD = 1.4826


def iqr_bounds(values: pd.Series, k: float = 1.5) -> tuple[float, float]:
    """Return the (lower, upper) Tukey fences for `values`."""
    q1, q3 = values.quantile(0.25), values.quantile(0.75)
    iqr = q3 - q1
    return q1 - k * iqr, q3 + k * iqr


def flag_iqr_outliers(values: pd.Series, k: float = 1.5) -> pd.Series:
    """Boolean mask, aligned to `values`, of points outside the IQR fences."""
    lower, upper = iqr_bounds(values, k=k)
    return (values < lower) | (values > upper)


def robust_zscores(values: pd.Series) -> pd.Series:
    """Median/MAD-based z-score — robust to the outliers it's measuring.

    Ordinary z-scores use the mean and standard deviation, both of which are
    themselves distorted by outliers. `values` with zero MAD (e.g. mostly
    identical amounts) get a score of 0 rather than a division by zero.
    """
    median = values.median()
    mad = (values - median).abs().median()
    if mad == 0:
        return pd.Series(0.0, index=values.index)
    return (values - median) / (mad * _MAD_TO_STD)


def robust_zscore_of(value: float, median: float, mad: float) -> float:
    """Robust z-score of a single `value` against a precomputed median/MAD.

    Unlike `robust_zscores`, the reference statistics are passed in directly
    rather than derived from a series that includes `value` itself. This is
    what lets a caller score a transaction against a *history that excludes
    it* (PR-009's historical-context requirement) instead of against a
    distribution it helped shape.

    A MAD of zero means the reference history had no variation at all (e.g.
    a subscription charged the exact same amount every time). A `value`
    that still matches it is unremarkable (score `0.0`); one that doesn't is
    a sharper break from a perfectly stable pattern than any finite z-score
    could represent, so it is scored as `+inf`/`-inf` rather than silently
    suppressed to 0 the way a naive division guard would.
    """
    if pd.isna(median) or pd.isna(mad):
        return float("nan")
    if mad == 0:
        if value == median:
            return 0.0
        return float("inf") if value > median else float("-inf")
    return (value - median) / (mad * _MAD_TO_STD)


def flag_category_relative_outliers(
    frame: pd.DataFrame,
    amount_column: str = "amount",
    category_column: str = "category",
    k: float = 1.5,
) -> pd.Series:
    """Boolean mask of rows that are IQR outliers within their own category.

    A transaction unremarkable in absolute terms can be unusual for its
    category (and vice versa) — comparing within category is what makes
    this "category-relative" rather than a single global threshold.
    """
    return frame.groupby(category_column)[amount_column].transform(
        lambda values: flag_iqr_outliers(values, k=k)
    )
