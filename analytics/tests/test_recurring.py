import pandas as pd
import pytest

from finance_analytics.analysis.recurring import recurring_candidates


@pytest.fixture
def transactions_frame() -> pd.DataFrame:
    return pd.DataFrame(
        {
            "date": pd.to_datetime(
                [
                    "2026-01-05",
                    "2026-02-05",
                    "2026-03-05",
                    "2026-01-20",
                    "2026-02-18",
                    "2026-01-10",
                ]
            ),
            "amount": [-9.99, -9.99, -9.99, -50.00, -75.00, -12.00],
            "merchant": [
                "Spotify",
                "Spotify",
                "Spotify",
                "Restaurant A",
                "Restaurant A",
                "One-off Shop",
            ],
        }
    )


def test_merchants_below_min_occurrences_are_excluded(transactions_frame):
    candidates = recurring_candidates(transactions_frame)

    assert "One-off Shop" not in set(candidates["merchant"])


def test_stable_amount_and_interval_merchant_has_low_variation(transactions_frame):
    candidates = recurring_candidates(transactions_frame).set_index("merchant")

    spotify = candidates.loc["Spotify"]
    assert spotify["occurrences"] == 3
    assert spotify["amount_variation"] == pytest.approx(0.0)
    assert spotify["median_interval_days"] == pytest.approx(30, abs=2)


def test_variable_merchant_has_higher_amount_variation_than_stable_one(transactions_frame):
    candidates = recurring_candidates(transactions_frame).set_index("merchant")

    assert (
        candidates.loc["Restaurant A", "amount_variation"]
        > candidates.loc["Spotify", "amount_variation"]
    )


def test_no_candidates_returns_expected_columns():
    frame = pd.DataFrame(
        {
            "date": pd.to_datetime(["2026-01-05"]),
            "amount": [-9.99],
            "merchant": ["Only Once"],
        }
    )

    candidates = recurring_candidates(frame)

    assert list(candidates.columns) == [
        "merchant",
        "occurrences",
        "amount_variation",
        "median_interval_days",
        "interval_variation",
    ]
    assert candidates.empty
