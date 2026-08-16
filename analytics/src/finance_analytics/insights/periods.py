"""Comparable month-over-month comparison windows (PR-012 §8).

PR-008's EDA found that this project's own fixture has exactly two observed
calendar months, and they are *not* directly comparable: February is a full
month, March is not — the export ends 2026-03-19, roughly two-thirds of the
way through the month (`notebooks/02_exploratory_data_analysis.ipynb`,
section 4: "March is a partial month, not a shorter one... Month-over-month
comparison is not meaningful again until a second full month of data
exists"). PR-012 §8 requires the same discipline generally: "Do not compare
non-comparable periods. For partial periods, either use comparable
partial-period analysis or suppress the insight."

This module always compares the *latest two calendar months present in the
data* (there is no wall-clock "today" in a CSV export — the latest
transaction date is the only signal available). It picks one of two
strategies:

- **Full month vs full month** — used when the current period's last
  observed day is that month's actual last calendar day, i.e. nothing
  suggests the export simply stopped mid-month.
- **Comparable partial period** — used otherwise. Both months are truncated
  to the same day-of-month cutoff (the current month's last observed day,
  capped at the previous month's own length) before comparing, so a partial
  current month is never compared against a full previous one.

`rules.py`'s insight functions all build on `build_period_comparison` /
`windowed_frames` / `comparison_confidence` rather than re-deriving month
boundaries themselves.
"""

from __future__ import annotations

from dataclasses import dataclass

import pandas as pd

#: Confidence multiplier for a comparable-partial comparison, relative to a
#: full-month one (`comparison_confidence`) — a partial window is real
#: evidence, but noisier than a full month by construction, so it is never
#: allowed to reach as high a confidence as a full-month comparison with
#: the same transaction volume.
FULL_MONTH_CONFIDENCE_FACTOR = 1.0
COMPARABLE_PARTIAL_CONFIDENCE_FACTOR = 0.6

#: Combined transaction count (current window + previous window) at which
#: the evidence-volume component of confidence saturates at 1.0. Six is
#: `rules.MIN_CATEGORY_TRANSACTIONS` (2) doubled for two windows, doubled
#: again as a round, defensible "plenty of evidence" floor — evidence-
#: motivated, not statistically fitted, consistent with every other
#: threshold in this codebase (see `anomalies.detector`,
#: `recurring.scoring`).
EVIDENCE_CONFIDENCE_TARGET = 6

#: Weights for `comparison_confidence`'s two signals. Sum to 1.0. Period
#: comparability is weighted higher than raw evidence volume: a large
#: transaction count cannot fix an inherently non-comparable pairing of
#: windows, but a comparable pairing with modest volume is still
#: meaningful evidence.
PERIOD_COMPLETENESS_WEIGHT = 0.6
EVIDENCE_WEIGHT = 0.4


@dataclass(frozen=True)
class PeriodComparison:
    """A resolved current-vs-previous month comparison window."""

    current_period: str
    previous_period: str
    is_current_period_complete: bool
    day_cutoff: int | None
    comparison_kind: str  # "full_month" | "comparable_partial"

    @property
    def label(self) -> str:
        """Human-readable phrase describing what the comparison covers,
        used directly in every rule's `description` text — honest about a
        partial comparison rather than silently implying a full month."""
        if self.comparison_kind == "full_month":
            return "last month"
        return f"the same {self.day_cutoff}-day period last month"


def build_period_comparison(
    frame: pd.DataFrame, date_column: str = "date"
) -> PeriodComparison | None:
    """Resolve the current-vs-previous month comparison window for `frame`.

    Returns `None` when fewer than two distinct calendar months of valid
    dates are present — there is nothing to compare against yet (PR-012's
    minimum-history requirement, applied to temporal comparisons). Rows
    with a missing/invalid date do not count towards either month.
    """
    dates = frame[date_column].dropna()
    if dates.empty:
        return None

    periods = sorted(dates.dt.to_period("M").unique())
    if len(periods) < 2:
        return None

    current, previous = periods[-1], periods[-2]
    last_day_in_data = int(dates[dates.dt.to_period("M") == current].dt.day.max())
    is_complete = last_day_in_data == current.days_in_month

    if is_complete:
        return PeriodComparison(
            current_period=str(current),
            previous_period=str(previous),
            is_current_period_complete=True,
            day_cutoff=None,
            comparison_kind="full_month",
        )

    day_cutoff = min(last_day_in_data, previous.days_in_month)
    return PeriodComparison(
        current_period=str(current),
        previous_period=str(previous),
        is_current_period_complete=False,
        day_cutoff=day_cutoff,
        comparison_kind="comparable_partial",
    )


def windowed_frames(
    frame: pd.DataFrame, comparison: PeriodComparison, date_column: str = "date"
) -> tuple[pd.DataFrame, pd.DataFrame]:
    """Split `frame` into `(current_window, previous_window)` rows matching
    `comparison`'s resolved boundaries: day-cutoff-truncated for a
    comparable-partial comparison, full calendar months otherwise. Rows
    with a missing/invalid date are excluded from both windows.
    """
    dates = frame[date_column]
    valid = dates.notna()
    periods = dates.dt.to_period("M")

    current_mask = valid & (periods == pd.Period(comparison.current_period))
    previous_mask = valid & (periods == pd.Period(comparison.previous_period))

    if comparison.day_cutoff is not None:
        current_mask &= dates.dt.day <= comparison.day_cutoff
        previous_mask &= dates.dt.day <= comparison.day_cutoff

    return frame[current_mask], frame[previous_mask]


def comparison_confidence(comparison: PeriodComparison, evidence_count: int) -> float:
    """Confidence for a period-comparison insight (PR-012 §4): blends how
    comparable the two windows are (full month vs truncated partial) with
    how much data actually backs the comparison — the same weighted-signal
    shape as `recurring.scoring.combine_confidence`, applied to a different
    pair of signals. Not a calibrated probability.
    """
    completeness_factor = (
        FULL_MONTH_CONFIDENCE_FACTOR
        if comparison.comparison_kind == "full_month"
        else COMPARABLE_PARTIAL_CONFIDENCE_FACTOR
    )
    evidence_factor = min(evidence_count / EVIDENCE_CONFIDENCE_TARGET, 1.0)
    score = PERIOD_COMPLETENESS_WEIGHT * completeness_factor + EVIDENCE_WEIGHT * evidence_factor
    return round(score, 2)
