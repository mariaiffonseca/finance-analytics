import math

import pandas as pd
import pytest

from finance_analytics.recurring.features import build_candidate_features


def _expense_row(id_, date, amount, merchant="Spotify", currency="EUR"):
    return {
        "id": id_,
        "date": pd.Timestamp(date),
        "amount": amount,
        "merchant": merchant,
        "currency": currency,
    }


def test_repeated_merchant_creates_a_candidate():
    frame = pd.DataFrame(
        [
            _expense_row("1", "2026-01-05", -9.99),
            _expense_row("2", "2026-02-04", -9.99),
        ]
    )

    features = build_candidate_features(frame).set_index("merchant")

    assert "Spotify" in features.index
    assert features.loc["Spotify", "occurrences"] == 2


def test_insufficient_history_is_handled_correctly():
    # A single occurrence still produces a row — occurrences=1, NaN
    # variation stats — rather than being silently dropped, so the detector
    # can classify it explicitly instead of it vanishing from the output.
    frame = pd.DataFrame([_expense_row("1", "2026-01-05", -9.99, merchant="Netflix")])

    features = build_candidate_features(frame).set_index("merchant")

    netflix = features.loc["Netflix"]
    assert netflix["occurrences"] == 1
    assert math.isnan(netflix["amount_variation"])
    assert math.isnan(netflix["median_interval_days"])
    assert math.isnan(netflix["interval_variation"])


def test_interval_variation_is_nan_with_exactly_two_occurrences():
    # Two occurrences give exactly one interval — nothing to compute
    # *variation* against (PR-008's EDA reaches the same structural
    # conclusion, not a bug).
    frame = pd.DataFrame(
        [
            _expense_row("1", "2026-01-05", -9.99),
            _expense_row("2", "2026-02-02", -9.99),
        ]
    )

    features = build_candidate_features(frame).set_index("merchant")

    assert math.isnan(features.loc["Spotify", "interval_variation"])
    assert features.loc["Spotify", "median_interval_days"] == pytest.approx(28.0)


def test_interval_variation_is_computed_with_three_or_more_occurrences():
    frame = pd.DataFrame(
        [
            _expense_row("1", "2026-01-01", -9.99),
            _expense_row("2", "2026-01-31", -9.99),
            _expense_row("3", "2026-03-02", -9.99),
        ]
    )

    features = build_candidate_features(frame).set_index("merchant")

    assert not math.isnan(features.loc["Spotify", "interval_variation"])
    assert features.loc["Spotify", "interval_variation"] == pytest.approx(0.0)


def test_amount_variation_reflects_dispersion():
    stable = pd.DataFrame(
        [
            _expense_row("1", "2026-01-05", -9.99, merchant="Spotify"),
            _expense_row("2", "2026-02-04", -9.99, merchant="Spotify"),
        ]
    )
    variable = pd.DataFrame(
        [
            _expense_row("3", "2026-01-07", -20.0, merchant="Restaurant"),
            _expense_row("4", "2026-02-05", -80.0, merchant="Restaurant"),
        ]
    )
    frame = pd.concat([stable, variable], ignore_index=True)

    features = build_candidate_features(frame).set_index("merchant")

    assert features.loc["Spotify", "amount_variation"] == pytest.approx(0.0)
    assert features.loc["Restaurant", "amount_variation"] > 0


def test_income_rows_are_excluded():
    frame = pd.DataFrame(
        [
            _expense_row("1", "2026-01-05", -9.99, merchant="Spotify"),
            {
                "id": "2",
                "date": pd.Timestamp("2026-01-31"),
                "amount": 3000.0,
                "merchant": "Employer Payroll",
                "currency": "EUR",
            },
        ]
    )

    features = build_candidate_features(frame)

    assert "Employer Payroll" not in set(features["merchant"])


def test_transaction_ids_are_attached_to_the_candidate():
    frame = pd.DataFrame(
        [
            _expense_row("a", "2026-01-05", -9.99),
            _expense_row("b", "2026-02-04", -9.99),
        ]
    )

    features = build_candidate_features(frame).set_index("merchant")

    assert features.loc["Spotify", "transaction_ids"] == ["a", "b"]


def test_no_expense_rows_returns_expected_columns():
    frame = pd.DataFrame(
        [
            {
                "id": "1",
                "date": pd.Timestamp("2026-01-05"),
                "amount": 100.0,
                "merchant": "Employer",
                "currency": "EUR",
            }
        ]
    )

    features = build_candidate_features(frame)

    assert list(features.columns) == [
        "merchant",
        "currency",
        "occurrences",
        "total_amount",
        "median_amount",
        "amount_variation",
        "median_interval_days",
        "interval_variation",
        "first_seen",
        "last_seen",
        "transaction_ids",
    ]
    assert features.empty


def test_rows_with_missing_merchant_are_dropped():
    frame = pd.DataFrame(
        [
            _expense_row("1", "2026-01-05", -9.99, merchant=None),
            _expense_row("2", "2026-02-04", -9.99),
        ]
    )

    features = build_candidate_features(frame)

    assert list(features["merchant"]) == ["Spotify"]
