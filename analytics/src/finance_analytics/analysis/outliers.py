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

#: Amounts are recorded to the cent. When a baseline's MAD is exactly zero
#: (e.g. an identical subscription charge every time, or a history where one
#: outlier doesn't shift the median off a repeated value), there is no
#: measured spread to scale a deviation against. Flooring MAD at one cent —
#: the smallest difference this data can represent — keeps a value that
#: matches the baseline at score 0.0 and a one-cent rounding difference
#: near 0.0, while a genuinely different amount still scores sharply
#: anomalous, instead of every non-exact match scoring an unbounded
#: infinite z-score regardless of how small the difference actually is.
_ZERO_MAD_FLOOR = 0.01


def iqr_bounds(values: pd.Series, k: float = 1.5) -> tuple[float, float]:
    """Return the (lower, upper) Tukey fences for `values`."""
    q1, q3 = values.quantile(0.25), values.quantile(0.75)
    iqr = q3 - q1
    return q1 - k * iqr, q3 + k * iqr


def flag_iqr_outliers(values: pd.Series, k: float = 1.5) -> pd.Series:
    """Boolean mask, aligned to `values`, of points outside the IQR fences."""
    lower, upper = iqr_bounds(values, k=k)
    return (values < lower) | (values > upper)


def _robust_zscore(value, median: float, mad: float):
    """Shared scoring primitive behind both `robust_zscores` and
    `robust_zscore_of`, so the two can't independently diverge on how a
    zero MAD is handled (as they previously did: one scored everything 0,
    the other scored any non-exact match as an unbounded infinity).
    """
    effective_mad = mad if mad > 0 else _ZERO_MAD_FLOOR
    return (value - median) / (effective_mad * _MAD_TO_STD)


def robust_zscores(values: pd.Series) -> pd.Series:
    """Median/MAD-based z-score — robust to the outliers it's measuring.

    Ordinary z-scores use the mean and standard deviation, both of which are
    themselves distorted by outliers. `values` with zero MAD (e.g. mostly
    identical amounts) are scored against a floored MAD (see
    `_ZERO_MAD_FLOOR`) rather than dividing by zero.
    """
    median = values.median()
    mad = (values - median).abs().median()
    return _robust_zscore(values, median, mad)


def robust_zscore_of(value: float, median: float, mad: float) -> float:
    """Robust z-score of a single `value` against a precomputed median/MAD.

    Unlike `robust_zscores`, the reference statistics are passed in directly
    rather than derived from a series that includes `value` itself. This is
    what lets a caller score a transaction against a *history that excludes
    it* (PR-009's historical-context requirement) instead of against a
    distribution it helped shape.

    A MAD of zero means the reference history had no variation at all (e.g.
    a subscription charged the exact same amount every time). Rather than
    scoring any non-exact match as an unbounded infinity — which would make
    a one-cent rounding difference score as maximally anomalous as a
    wildly different amount — the MAD is floored at one cent (see
    `_ZERO_MAD_FLOOR`), so a near-identical value scores close to 0 and a
    genuinely different one still scores sharply anomalous.
    """
    if pd.isna(median) or pd.isna(mad):
        return float("nan")
    return _robust_zscore(value, median, mad)


def percentile_of_sorted(sorted_values: list[float], q: float) -> float:
    """Linear-interpolated percentile of an already-sorted list of floats.

    The shared primitive behind every median/MAD/IQR calculation on a
    plain (non-`pd.Series`) sequence in this codebase — in particular
    `anomalies.features._median_mad_iqr`, which maintains its per-merchant
    and per-category histories pre-sorted for O(1) lookup instead of
    resorting them from scratch on every transaction. Matches the linear
    interpolation `pd.Series.quantile`'s default method uses, so results
    agree with the rest of this module.
    """
    n = len(sorted_values)
    h = (n - 1) * q
    lo = int(h)
    hi = min(lo + 1, n - 1)
    frac = h - lo
    return sorted_values[lo] + (sorted_values[hi] - sorted_values[lo]) * frac


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
