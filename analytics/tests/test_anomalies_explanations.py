from finance_analytics.anomalies.explanations import (
    explain_category_relative,
    explain_global_relative,
    explain_insufficient_history,
    explain_merchant_relative,
)


def test_flagged_category_explanation_references_category_and_ratio():
    reason = explain_category_relative("Shopping", amount=380.0, median=100.0, is_anomaly=True)

    assert "Shopping" in reason
    assert "3.8×" in reason


def test_flagged_merchant_explanation_references_merchant():
    reason = explain_merchant_relative("Spotify", amount=59.99, median=29.99, is_anomaly=True)

    assert "Spotify" in reason


def test_flagged_global_explanation_has_no_category_or_merchant_name():
    reason = explain_global_relative(amount=200.0, median=50.0, is_anomaly=True)

    assert "4.0×" in reason


def test_not_flagged_explanation_still_references_context():
    reason = explain_category_relative("Groceries", amount=11.0, median=10.0, is_anomaly=False)

    assert "Groceries" in reason
    assert "×" not in reason


def test_zero_median_falls_back_to_a_ratio_free_explanation():
    reason = explain_category_relative("Groceries", amount=10.0, median=0.0, is_anomaly=True)

    assert "×" not in reason
    assert "Groceries" in reason


def test_insufficient_history_explanation_is_deterministic_and_references_counts():
    reason = explain_insufficient_history(category_count=1, merchant_count=0, global_count=2)

    assert reason == explain_insufficient_history(
        category_count=1, merchant_count=0, global_count=2
    )
    assert "1" in reason
    assert "0" in reason
    assert "2" in reason


def test_explanation_uses_the_given_currency_not_a_hardcoded_symbol():
    reason = explain_merchant_relative(
        "Spotify", amount=59.99, median=29.99, is_anomaly=True, currency="USD"
    )

    assert "USD" in reason
    assert "€" not in reason


def test_ratio_rounding_to_no_visible_change_falls_back_to_qualitative_text():
    # z-score flags this (small MAD baseline) but amount/median rounds to
    # "1.0×", which would read as "no different from typical" — a
    # self-contradiction the qualitative fallback avoids.
    reason = explain_merchant_relative("Landlord", amount=102.8, median=100.5, is_anomaly=True)

    assert "1.0×" not in reason
    assert "Landlord" in reason


def test_not_anomalous_but_unusually_low_does_not_claim_close_to_typical():
    reason = explain_merchant_relative(
        "Merchant", amount=5.0, median=29.99, is_anomaly=False, is_unusually_low=True
    )

    assert "close to" not in reason
    assert "0.2×" in reason


def test_nan_median_does_not_render_literal_nan():
    reason = explain_category_relative(
        "Groceries", amount=15.0, median=float("nan"), is_anomaly=True
    )

    assert "nan" not in reason.lower()
    assert "Groceries" in reason
