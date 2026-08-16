import math

import pandas as pd
import pytest

from finance_analytics.anomalies.features import build_historical_features


@pytest.fixture
def transactions_frame() -> pd.DataFrame:
    return pd.DataFrame(
        {
            "id": ["1", "2", "3", "4", "5"],
            "date": pd.to_datetime(
                ["2026-01-01", "2026-01-05", "2026-01-10", "2026-01-15", "2026-01-20"]
            ),
            "amount": [-10.0, -12.0, -900.0, 2000.0, -11.0],
            "category": ["Groceries", "Groceries", "Shopping", "Income", "Groceries"],
            "merchant": ["Continente", "Continente", "MediaMarkt", "Employer", "Continente"],
        }
    )


def test_income_rows_are_excluded(transactions_frame):
    features = build_historical_features(transactions_frame)

    assert "Income" not in set(features["category"])


def test_first_transaction_has_no_history(transactions_frame):
    features = build_historical_features(transactions_frame)

    first = features.iloc[0]
    assert first["merchant_history_count"] == 0
    assert first["category_history_count"] == 0
    assert first["global_history_count"] == 0
    assert math.isnan(first["merchant_median"])
    assert math.isnan(first["category_median"])
    assert math.isnan(first["global_median"])


def test_history_counts_and_medians_accumulate_chronologically(transactions_frame):
    features = build_historical_features(transactions_frame).set_index("id")

    # Third Continente/Groceries transaction (id 5): two prior same-merchant
    # and same-category transactions (10.0, 12.0), plus one unrelated prior
    # expense (MediaMarkt, 900.0) counted only in the global history.
    third = features.loc["5"]
    assert third["merchant_history_count"] == 2
    assert third["merchant_median"] == pytest.approx(11.0)
    assert third["category_history_count"] == 2
    assert third["category_median"] == pytest.approx(11.0)
    assert third["global_history_count"] == 3


def test_current_row_never_contributes_to_its_own_baseline(transactions_frame):
    features = build_historical_features(transactions_frame).set_index("id")

    # id 2 is Continente's second transaction: history must reflect only id 1.
    second = features.loc["2"]
    assert second["merchant_history_count"] == 1
    assert second["merchant_median"] == pytest.approx(10.0)


def test_a_later_transaction_does_not_change_an_earlier_ones_features(transactions_frame):
    without_later_row = transactions_frame.iloc[:-1]

    features_with_all_rows = build_historical_features(transactions_frame).set_index("id")
    features_without_later_row = build_historical_features(without_later_row).set_index("id")

    early_id = "2"
    pd.testing.assert_series_equal(
        features_with_all_rows.loc[early_id], features_without_later_row.loc[early_id]
    )


def test_rows_with_missing_category_are_dropped_not_pooled():
    frame = pd.DataFrame(
        {
            "id": ["1", "2", "3"],
            "date": pd.to_datetime(["2026-01-01", "2026-01-02", "2026-01-03"]),
            "amount": [-10.0, -12.0, -11.0],
            "category": ["Groceries", None, "Groceries"],
            "merchant": ["Continente", "UnknownShop", "Continente"],
        }
    )

    features = build_historical_features(frame)

    assert set(features["id"]) == {"1", "3"}


def test_global_iqr_is_included_alongside_category_iqr():
    frame = pd.DataFrame(
        {
            "id": ["1", "2", "3"],
            "date": pd.to_datetime(["2026-01-01", "2026-01-02", "2026-01-03"]),
            "amount": [-10.0, -12.0, -11.0],
            "category": ["Groceries", "Groceries", "Groceries"],
            "merchant": ["Continente", "Continente", "Continente"],
        }
    )

    features = build_historical_features(frame)

    assert "global_iqr" in features.columns
