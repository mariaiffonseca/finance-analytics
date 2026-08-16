import pandas as pd

from finance_analytics.recurring.detector import (
    MIN_OCCURRENCES_FOR_RECURRING,
    RecurringResult,
    detect_recurring_transactions,
)


def _expense_row(id_, date, amount, merchant, currency="EUR"):
    return {
        "id": id_,
        "date": pd.Timestamp(date),
        "amount": amount,
        "merchant": merchant,
        "currency": currency,
    }


def _result_for(results: list[RecurringResult], merchant: str) -> RecurringResult:
    return next(r for r in results if r.merchant == merchant)


def test_clear_recurring_subscription_is_classified_recurring():
    # Identical amount, exact monthly cadence, 5 occurrences.
    dates = pd.date_range("2026-01-01", periods=5, freq="30D")
    frame = pd.DataFrame([_expense_row(str(i), d, -9.99, "Spotify") for i, d in enumerate(dates)])

    result = _result_for(detect_recurring_transactions(frame), "Spotify")

    assert result.classification == "Recurring"
    assert result.is_recurring is True
    assert result.frequency == "Monthly"
    assert result.confidence_score >= 0.70


def test_two_occurrences_with_tight_signals_is_possible_recurring_not_recurring():
    # Mirrors the real EDA fixture's Spotify candidate: identical amount,
    # ~28-day gap, but only 2 occurrences — interval consistency cannot yet
    # be confirmed, so this must not reach "Recurring".
    frame = pd.DataFrame(
        [
            _expense_row("1", "2026-01-05", -29.99, "Spotify"),
            _expense_row("2", "2026-02-02", -29.99, "Spotify"),
        ]
    )

    result = _result_for(detect_recurring_transactions(frame), "Spotify")

    assert result.classification == "Possible recurring"
    assert result.is_recurring is False
    assert result.occurrences < MIN_OCCURRENCES_FOR_RECURRING


def test_three_occurrences_with_moderate_consistency_is_possible_recurring():
    # Interval and amount both drift enough (confidence ~0.54) to miss the
    # "Recurring" bar despite clearing the 3-occurrence structural floor —
    # the confidence-based route to "Possible recurring", distinct from the
    # 2-occurrence structural-cap route covered above.
    frame = pd.DataFrame(
        [
            _expense_row("1", "2026-01-01", -30.0, "Gym"),
            _expense_row("2", "2026-01-27", -40.0, "Gym"),
            _expense_row("3", "2026-03-05", -35.0, "Gym"),
        ]
    )

    result = _result_for(detect_recurring_transactions(frame), "Gym")

    assert result.classification == "Possible recurring"
    assert result.is_recurring is False
    assert result.occurrences >= MIN_OCCURRENCES_FOR_RECURRING


def test_frequent_but_irregular_merchant_is_not_recurring():
    # Many visits, wildly different amounts and gaps — a repeated merchant
    # that must not be assumed recurring (PR-010's Critical Rule).
    dates = ["2026-01-01", "2026-01-06", "2026-01-18", "2026-02-16", "2026-03-08"]
    amounts = [-10.0, -25.0, -8.0, -40.0, -15.0]
    frame = pd.DataFrame(
        [
            _expense_row(str(i), d, a, "Coffee Corner")
            for i, (d, a) in enumerate(zip(dates, amounts))
        ]
    )

    result = _result_for(detect_recurring_transactions(frame), "Coffee Corner")

    assert result.classification == "Not recurring"
    assert result.is_recurring is False
    assert result.occurrences >= MIN_OCCURRENCES_FOR_RECURRING


def test_one_off_transaction_is_not_treated_as_recurring():
    frame = pd.DataFrame([_expense_row("1", "2026-01-05", -900.0, "MediaMarkt")])

    result = _result_for(detect_recurring_transactions(frame), "MediaMarkt")

    assert result.is_recurring is False
    assert result.classification == "Insufficient history"


def test_insufficient_history_is_flagged_explicitly_not_scored():
    frame = pd.DataFrame([_expense_row("1", "2026-01-05", -15.99, "Netflix")])

    result = _result_for(detect_recurring_transactions(frame), "Netflix")

    assert result.classification == "Insufficient history"
    assert result.confidence_score is None
    assert "Netflix" in result.reason


def test_small_amount_variation_is_tolerated():
    dates = pd.date_range("2026-01-01", periods=4, freq="30D")
    amounts = [-9.99, -9.99, -10.49, -9.99]  # one small bump
    frame = pd.DataFrame(
        [_expense_row(str(i), d, a, "Spotify") for i, (d, a) in enumerate(zip(dates, amounts))]
    )

    result = _result_for(detect_recurring_transactions(frame), "Spotify")

    assert result.classification == "Recurring"


def test_large_amount_variation_reduces_confidence_below_a_tight_match():
    dates = pd.date_range("2026-01-01", periods=4, freq="30D")
    tight_frame = pd.DataFrame(
        [_expense_row(str(i), d, -9.99, "Spotify") for i, d in enumerate(dates)]
    )
    variable_amounts = [-9.99, -25.0, -5.0, -40.0]
    variable_frame = pd.DataFrame(
        [
            _expense_row(str(i), d, a, "Spotify")
            for i, (d, a) in enumerate(zip(dates, variable_amounts))
        ]
    )

    tight = _result_for(detect_recurring_transactions(tight_frame), "Spotify")
    variable = _result_for(detect_recurring_transactions(variable_frame), "Spotify")

    assert tight.confidence_score > variable.confidence_score


def test_income_rows_are_not_classified():
    frame = pd.DataFrame(
        [
            _expense_row("1", "2026-01-05", -9.99, "Spotify"),
            {
                "id": "2",
                "date": pd.Timestamp("2026-01-31"),
                "amount": 3000.0,
                "merchant": "Employer Payroll",
                "currency": "EUR",
            },
        ]
    )

    results = detect_recurring_transactions(frame)

    assert "Employer Payroll" not in {r.merchant for r in results}


def test_results_are_deterministic():
    dates = pd.date_range("2026-01-01", periods=4, freq="30D")
    frame = pd.DataFrame([_expense_row(str(i), d, -9.99, "Spotify") for i, d in enumerate(dates)])

    first_run = detect_recurring_transactions(frame)
    second_run = detect_recurring_transactions(frame)

    assert first_run == second_run


def test_every_classified_candidate_has_a_deterministic_explanation():
    dates = pd.date_range("2026-01-01", periods=4, freq="30D")
    frame = pd.DataFrame(
        [
            *[_expense_row(str(i), d, -9.99, "Spotify") for i, d in enumerate(dates)],
            _expense_row("s1", "2026-01-05", -29.99, "OneOff"),
        ]
    )

    for result in detect_recurring_transactions(frame):
        assert result.reason
        assert isinstance(result.reason, str)


def test_transaction_ids_are_included_in_the_result():
    frame = pd.DataFrame(
        [
            _expense_row("a", "2026-01-05", -9.99, "Spotify"),
            _expense_row("b", "2026-02-02", -9.99, "Spotify"),
        ]
    )

    result = _result_for(detect_recurring_transactions(frame), "Spotify")

    assert result.transaction_ids == ["a", "b"]
