from finance_analytics.recurring.explanations import (
    explain_insufficient_history,
    explain_not_recurring,
    explain_possible_recurring,
    explain_recurring,
)


def test_recurring_explanation_names_merchant_frequency_and_amount():
    reason = explain_recurring(
        merchant="Spotify",
        occurrences=4,
        median_interval_days=30.0,
        frequency="Monthly",
        median_amount=9.99,
        currency="EUR",
        amount_variation=0.0,
    )

    assert "Spotify" in reason
    assert "monthly" in reason.lower()
    assert "9.99 EUR" in reason


def test_recurring_explanation_uses_qualitative_phrase_for_varying_amount():
    reason = explain_recurring(
        merchant="Gym",
        occurrences=4,
        median_interval_days=30.0,
        frequency="Monthly",
        median_amount=40.0,
        currency="EUR",
        amount_variation=0.12,
    )

    assert "similar amount" in reason


def test_possible_recurring_explanation_mentions_the_missing_occurrence():
    reason = explain_possible_recurring(
        merchant="Spotify",
        occurrences=2,
        median_interval_days=28.0,
        median_amount=9.99,
        currency="EUR",
        min_occurrences_for_recurring=3,
    )

    assert "Spotify" in reason
    assert "3" in reason


def test_possible_recurring_explanation_for_moderate_confidence_at_three_occurrences():
    reason = explain_possible_recurring(
        merchant="Gym",
        occurrences=3,
        median_interval_days=30.0,
        median_amount=40.0,
        currency="EUR",
        min_occurrences_for_recurring=3,
    )

    assert "Gym" in reason
    assert "not strong enough" in reason


def test_not_recurring_explanation_references_both_variations():
    reason = explain_not_recurring(
        merchant="Coffee Corner",
        occurrences=6,
        amount_variation=0.55,
        interval_variation=0.75,
    )

    assert "Coffee Corner" in reason
    assert "55%" in reason
    assert "75%" in reason


def test_not_recurring_explanation_handles_nan_interval_variation():
    reason = explain_not_recurring(
        merchant="Coffee Corner",
        occurrences=6,
        amount_variation=0.55,
        interval_variation=float("nan"),
    )

    assert "nan" not in reason.lower()
    assert "Coffee Corner" in reason


def test_insufficient_history_explanation_is_deterministic():
    reason = explain_insufficient_history("Netflix", occurrences=1, min_occurrences=2)

    assert reason == explain_insufficient_history("Netflix", occurrences=1, min_occurrences=2)
    assert "Netflix" in reason
    assert "1 transaction" in reason
    assert "2" in reason


def test_insufficient_history_explanation_pluralises_correctly():
    reason = explain_insufficient_history("Netflix", occurrences=1, min_occurrences=2)

    assert "1 transaction " in reason or reason.endswith("1 transaction")
    assert "1 transactions" not in reason
