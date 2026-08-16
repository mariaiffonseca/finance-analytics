import pandas as pd

from finance_analytics.insights.periods import (
    build_period_comparison,
    comparison_confidence,
    windowed_frames,
)


def _frame(dates: list[str]) -> pd.DataFrame:
    return pd.DataFrame({"date": [pd.Timestamp(d) for d in dates]})


def test_returns_none_with_a_single_month():
    frame = _frame(["2026-01-05", "2026-01-12", "2026-01-19"])
    assert build_period_comparison(frame) is None


def test_returns_none_with_no_valid_dates():
    frame = pd.DataFrame({"date": [pd.NaT, pd.NaT]})
    assert build_period_comparison(frame) is None


def test_full_month_when_last_transaction_lands_on_the_calendar_last_day():
    # February 2026 has 28 days (not a leap year); the last transaction
    # lands exactly on day 28, so nothing suggests the export stopped early.
    frame = _frame(["2026-01-05", "2026-02-04", "2026-02-28"])

    comparison = build_period_comparison(frame)

    assert comparison.current_period == "2026-02"
    assert comparison.previous_period == "2026-01"
    assert comparison.is_current_period_complete is True
    assert comparison.day_cutoff is None
    assert comparison.comparison_kind == "full_month"
    assert comparison.label == "last month"


def test_comparable_partial_when_current_month_stops_before_the_last_day():
    # Mirrors the project's own fixture: March data ends on day 19, and
    # March has 31 days, so this is a partial month, not a short one.
    frame = _frame(["2026-02-05", "2026-03-01", "2026-03-19"])

    comparison = build_period_comparison(frame)

    assert comparison.current_period == "2026-03"
    assert comparison.previous_period == "2026-02"
    assert comparison.is_current_period_complete is False
    assert comparison.day_cutoff == 19
    assert comparison.comparison_kind == "comparable_partial"
    assert "19-day period" in comparison.label


def test_day_cutoff_is_capped_at_the_previous_months_length():
    # Current month (April, 30 days) stops on day 29 — partial, not full —
    # and the previous month (February) only has 28 days, so the cutoff
    # cannot exceed 28 or it would reach into March.
    frame = _frame(["2026-02-10", "2026-04-05", "2026-04-29"])

    comparison = build_period_comparison(frame)

    assert comparison.day_cutoff == 28


def test_uses_the_latest_two_months_when_more_than_two_are_present():
    frame = _frame(["2025-11-01", "2025-12-01", "2026-01-05", "2026-02-28"])

    comparison = build_period_comparison(frame)

    assert comparison.current_period == "2026-02"
    assert comparison.previous_period == "2026-01"


def test_windowed_frames_full_month_includes_the_whole_calendar_month():
    frame = pd.DataFrame(
        {
            "date": [
                pd.Timestamp("2026-01-05"),
                pd.Timestamp("2026-02-01"),
                pd.Timestamp("2026-02-28"),
            ]
        }
    )
    comparison = build_period_comparison(frame)

    current, previous = windowed_frames(frame, comparison)

    assert len(current) == 2
    assert len(previous) == 1


def test_windowed_frames_comparable_partial_truncates_both_windows():
    frame = pd.DataFrame(
        {
            "date": [
                pd.Timestamp("2026-02-05"),
                pd.Timestamp("2026-02-25"),  # excluded: after the day-19 cutoff
                pd.Timestamp("2026-03-01"),
                pd.Timestamp("2026-03-19"),
            ]
        }
    )
    comparison = build_period_comparison(frame)

    current, previous = windowed_frames(frame, comparison)

    assert len(current) == 2
    assert len(previous) == 1  # the day-25 February row is excluded


def test_comparison_confidence_full_month_beats_comparable_partial():
    full = comparison_confidence(
        build_period_comparison(_frame(["2026-01-05", "2026-02-01", "2026-02-28"])),
        evidence_count=6,
    )
    partial = comparison_confidence(
        build_period_comparison(_frame(["2026-02-05", "2026-03-01", "2026-03-19"])),
        evidence_count=6,
    )

    assert full > partial
    assert full == 1.0
    assert partial == 0.76


def test_comparison_confidence_saturates_with_more_evidence():
    comparison = build_period_comparison(_frame(["2026-01-05", "2026-02-01", "2026-02-28"]))

    low_evidence = comparison_confidence(comparison, evidence_count=1)
    high_evidence = comparison_confidence(comparison, evidence_count=100)

    assert low_evidence < high_evidence
    assert high_evidence == 1.0
