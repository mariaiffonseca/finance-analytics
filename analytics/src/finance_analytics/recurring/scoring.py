"""Deterministic confidence scoring for recurring-transaction candidates
(PR-010).

`combine_confidence` blends four signals into a single 0-1 score:

- **Interval consistency** — how stable the day-gaps between occurrences
  are.
- **Amount consistency** — how stable the charged amount is.
- **Occurrence strength** — how many times the merchant has been seen.
- **History-span strength** — how much elapsed time those occurrences
  actually cover.

This is a deterministic heuristic score, not a calibrated probability
(PR-010, Confidence Score — "Do not call it a calibrated probability unless
it actually is"). Nothing here has been fit or validated against labelled
outcomes, so `confidence_score` must never be read as "probability this is
a real subscription" — only as a relative ranking of how strong the
combined evidence is.

**Why these weights.** Justified against PR-008's EDA
(`notebooks/02_exploratory_data_analysis.ipynb`, section 8 / RQ5, backed by
`analysis.recurring.recurring_candidates`): interval and amount stability
are what actually distinguish a subscription from a frequent-but-irregular
merchant in that data — `Spotify` (0% amount variation, ~28-day gaps) reads
as a strong candidate, while `O Pescador` (~52% amount variation despite a
similar ~29-day gap) reads as casual dining out, not a fixed charge. Those
two signals carry the most weight. Occurrence count and history span
corroborate *how much evidence there is*, not *what the evidence shows*, so
they carry less.
"""

from __future__ import annotations

import math

#: Weight given to each signal in the combined confidence score. Sums to 1.0.
INTERVAL_WEIGHT = 0.35
AMOUNT_WEIGHT = 0.30
OCCURRENCE_WEIGHT = 0.20
HISTORY_SPAN_WEIGHT = 0.15

#: Coefficient of variation (std / mean) of transaction amounts at or above
#: which amount consistency scores 0. Anchored to the EDA fixture:
#: `O Pescador`'s ~52% variation (casual dining) is clearly not a fixed
#: charge, so the tolerance sits well below that; `Continente`'s ~7.8%
#: still scores highly at this tolerance.
AMOUNT_VARIATION_TOLERANCE = 0.30

#: Coefficient of variation of day-gaps at or above which interval
#: consistency scores 0. Kept wider than the amount tolerance: calendar
#: effects (weekends, billing-day drift, month length) make interval
#: timing inherently noisier than a fixed subscription price.
INTERVAL_VARIATION_TOLERANCE = 0.40

#: Consistency credit awarded when a signal cannot be computed at all —
#: currently only `interval_variation` with exactly 2 occurrences (one
#: interval, nothing to compute *variation* against). Neither "stable" nor
#: "unstable": PR-008's EDA found this a structural limit of small samples,
#: not evidence either way, so it is scored as a neutral midpoint rather
#: than rewarded or penalised.
UNKNOWN_SIGNAL_CREDIT = 0.5

#: Occurrence count at which the occurrence-strength signal reaches full
#: strength (1.0). Set above `detector.MIN_OCCURRENCES_FOR_RECURRING` (3)
#: so merely clearing the minimum for a "Recurring" classification doesn't
#: also max out this sub-score — a fourth occurrence is real additional
#: evidence.
OCCURRENCE_TARGET = 4

#: Number of full cadence cycles (`median_interval_days` each) the observed
#: span must cover for the history-span signal to reach full strength (1.0).
#: Matches the same "at least 3 occurrences / 2 intervals" bar PR-008's EDA
#: used for trusting an interval as *stable* rather than merely *present*.
MIN_CYCLES_FOR_FULL_CONFIDENCE = 3

#: (label, inclusive day range) bands for describing `median_interval_days`.
#: Ranges are tolerant, not exact-day matches (PR-010, Interval Analysis:
#: "do not assume 30 days == monthly exactly") — the EDA fixture's own
#: monthly merchants land at 28-29 days, not 30.
_FREQUENCY_BANDS: list[tuple[str, int, int]] = [
    ("Weekly", 6, 8),
    ("Biweekly", 12, 16),
    ("Monthly", 26, 35),
    ("Quarterly", 80, 100),
]


def amount_consistency(amount_variation: float) -> float:
    """0-1: 1.0 for an identical amount every time, 0.0 at/above the
    tolerance, linear in between."""
    if math.isnan(amount_variation):
        return UNKNOWN_SIGNAL_CREDIT
    return max(0.0, 1.0 - amount_variation / AMOUNT_VARIATION_TOLERANCE)


def interval_consistency(interval_variation: float) -> float:
    """0-1: 1.0 for perfectly evenly-spaced occurrences, 0.0 at/above the
    tolerance, `UNKNOWN_SIGNAL_CREDIT` when it cannot be computed (`NaN`,
    i.e. fewer than 2 intervals)."""
    if math.isnan(interval_variation):
        return UNKNOWN_SIGNAL_CREDIT
    return max(0.0, 1.0 - interval_variation / INTERVAL_VARIATION_TOLERANCE)


def occurrence_strength(occurrences: int) -> float:
    """0-1: how many times this merchant has been seen, saturating at
    `OCCURRENCE_TARGET`."""
    return min(occurrences / OCCURRENCE_TARGET, 1.0)


def history_span_strength(
    first_seen: object, last_seen: object, median_interval_days: float
) -> float:
    """0-1: how much elapsed time the occurrences cover, in units of
    `median_interval_days`-sized cycles, saturating at
    `MIN_CYCLES_FOR_FULL_CONFIDENCE` cycles.

    Distinct from `occurrence_strength`: a burst of several transactions in
    a couple of days would score high on occurrence count but low here,
    correctly signalling "many repeats, but no time depth to confirm a
    periodic cadence". `0.0` when `median_interval_days` is unavailable or
    non-positive (e.g. same-day duplicate transactions) — there is no
    cadence to measure a span against.
    """
    if math.isnan(median_interval_days) or median_interval_days <= 0:
        return 0.0
    span_days = (last_seen - first_seen).days
    return min(span_days / (median_interval_days * MIN_CYCLES_FOR_FULL_CONFIDENCE), 1.0)


def combine_confidence(
    *,
    amount_variation: float,
    interval_variation: float,
    occurrences: int,
    first_seen: object,
    last_seen: object,
    median_interval_days: float,
) -> float:
    """Combine the four signals into a single 0-1 confidence score.

    Deterministic: the same inputs always produce the same output. See the
    module docstring for why this is a heuristic score, not a probability.
    """
    score = (
        AMOUNT_WEIGHT * amount_consistency(amount_variation)
        + INTERVAL_WEIGHT * interval_consistency(interval_variation)
        + OCCURRENCE_WEIGHT * occurrence_strength(occurrences)
        + HISTORY_SPAN_WEIGHT * history_span_strength(first_seen, last_seen, median_interval_days)
    )
    return round(score, 4)


def classify_frequency(median_interval_days: float) -> str:
    """Describe `median_interval_days` using a tolerant band (PR-010,
    Recurring Definition: Weekly / Biweekly / Monthly / Quarterly / Other).

    `"Unknown"` when no interval is available at all (fewer than 2
    occurrences). `"Other regular interval"` when an interval exists but
    doesn't fall in a named band — still descriptive of a real, computed
    cadence, just not one of the common ones.
    """
    if math.isnan(median_interval_days):
        return "Unknown"
    for label, low, high in _FREQUENCY_BANDS:
        if low <= median_interval_days <= high:
            return label
    return "Other regular interval"
