"""Reusable temporal feature generation for transaction analysis.

These are calendar features derived from `date`, used throughout the EDA
notebook to investigate monthly patterns, weekday/weekend behaviour and
transaction frequency (see PR-008).
"""

from __future__ import annotations

import pandas as pd


def add_temporal_features(frame: pd.DataFrame, date_column: str = "date") -> pd.DataFrame:
    """Return a copy of `frame` with derived calendar columns.

    Adds `year`, `month`, `month_period` (the `"YYYY-MM"` the date falls in),
    `day_of_week` (weekday name) and `day_of_month`. Rows with a missing or
    invalid date (`NaT`) get missing values in these columns rather than
    being dropped — callers decide whether to exclude them.
    """
    result = frame.copy()
    dates = result[date_column]

    result["year"] = dates.dt.year
    result["month"] = dates.dt.month
    result["month_period"] = dates.dt.to_period("M").astype(str)
    result.loc[dates.isna(), "month_period"] = pd.NA
    result["day_of_week"] = dates.dt.day_name()
    result["day_of_month"] = dates.dt.day

    return result
