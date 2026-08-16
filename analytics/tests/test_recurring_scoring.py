import pandas as pd
import pytest

from finance_analytics.recurring.scoring import (
    amount_consistency,
    classify_frequency,
    combine_confidence,
    history_span_strength,
    interval_consistency,
    occurrence_strength,
)


def test_amount_consistency_is_perfect_for_identical_amounts():
    assert amount_consistency(0.0) == pytest.approx(1.0)


def test_amount_consistency_decreases_as_variation_increases():
    tight = amount_consistency(0.05)
    moderate = amount_consistency(0.15)
    wide = amount_consistency(0.52)  # O Pescador's EDA-observed variation

    assert tight > moderate > wide
    assert wide == pytest.approx(0.0)  # clamped, not negative


def test_amount_consistency_unknown_credit_for_nan():
    assert amount_consistency(float("nan")) == 0.5


def test_interval_consistency_is_perfect_for_zero_variation():
    assert interval_consistency(0.0) == pytest.approx(1.0)


def test_interval_consistency_unknown_credit_for_nan():
    # Structural NaN — exactly one interval, nothing to compute variation
    # against (2 occurrences). Neutral, not penalised.
    assert interval_consistency(float("nan")) == 0.5


def test_interval_consistency_decreases_as_variation_increases():
    stable = interval_consistency(0.05)
    unstable = interval_consistency(0.6)

    assert stable > unstable
    assert unstable == pytest.approx(0.0)


def test_occurrence_strength_saturates_at_target():
    assert occurrence_strength(4) == pytest.approx(1.0)
    assert occurrence_strength(10) == pytest.approx(1.0)
    assert occurrence_strength(2) < occurrence_strength(4)


def test_history_span_strength_saturates_after_enough_cycles():
    first = pd.Timestamp("2026-01-01")
    last_three_cycles = pd.Timestamp("2026-01-01") + pd.Timedelta(days=90)
    last_one_cycle = pd.Timestamp("2026-01-01") + pd.Timedelta(days=30)

    full = history_span_strength(first, last_three_cycles, median_interval_days=30.0)
    partial = history_span_strength(first, last_one_cycle, median_interval_days=30.0)

    assert full == pytest.approx(1.0)
    assert partial == pytest.approx(1 / 3)


def test_history_span_strength_is_zero_for_non_positive_interval():
    first = pd.Timestamp("2026-01-01")
    assert history_span_strength(first, first, median_interval_days=0.0) == 0.0
    assert history_span_strength(first, first, median_interval_days=float("nan")) == 0.0


def test_combine_confidence_is_deterministic():
    kwargs = {
        "amount_variation": 0.0,
        "interval_variation": 0.0,
        "occurrences": 4,
        "first_seen": pd.Timestamp("2026-01-01"),
        "last_seen": pd.Timestamp("2026-04-01"),
        "median_interval_days": 30.0,
    }

    assert combine_confidence(**kwargs) == combine_confidence(**kwargs)


def test_combine_confidence_is_highest_for_a_perfectly_stable_merchant():
    perfect = combine_confidence(
        amount_variation=0.0,
        interval_variation=0.0,
        occurrences=4,
        first_seen=pd.Timestamp("2026-01-01"),
        last_seen=pd.Timestamp("2026-04-01"),
        median_interval_days=30.0,
    )

    assert perfect == pytest.approx(1.0)


def test_combine_confidence_rewards_more_evidence_at_equal_consistency():
    two_occurrences = combine_confidence(
        amount_variation=0.0,
        interval_variation=float("nan"),
        occurrences=2,
        first_seen=pd.Timestamp("2026-01-01"),
        last_seen=pd.Timestamp("2026-01-29"),
        median_interval_days=28.0,
    )
    four_occurrences = combine_confidence(
        amount_variation=0.0,
        interval_variation=0.0,
        occurrences=4,
        first_seen=pd.Timestamp("2026-01-01"),
        last_seen=pd.Timestamp("2026-04-01"),
        median_interval_days=28.0,
    )

    assert four_occurrences > two_occurrences


def test_combine_confidence_penalises_frequent_but_irregular_merchant():
    irregular = combine_confidence(
        amount_variation=0.55,
        interval_variation=0.75,
        occurrences=6,
        first_seen=pd.Timestamp("2026-01-01"),
        last_seen=pd.Timestamp("2026-05-20"),
        median_interval_days=20.0,
    )

    assert irregular < 0.45  # below the "Possible recurring" bar


def test_classify_frequency_bands():
    assert classify_frequency(7) == "Weekly"
    assert classify_frequency(14) == "Biweekly"
    assert classify_frequency(28) == "Monthly"
    assert classify_frequency(90) == "Quarterly"
    assert classify_frequency(50) == "Other regular interval"


def test_classify_frequency_unknown_for_nan():
    assert classify_frequency(float("nan")) == "Unknown"
