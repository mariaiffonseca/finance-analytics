import pandas as pd
import pytest

from finance_analytics.analysis.temporal import add_temporal_features


@pytest.fixture
def transactions_frame() -> pd.DataFrame:
    return pd.DataFrame(
        {
            "date": pd.to_datetime(["2026-01-05", "2026-02-14", None]),
            "amount": [-12.50, -40.00, -15.99],
        }
    )


def test_adds_year_month_and_month_period(transactions_frame):
    result = add_temporal_features(transactions_frame)

    assert list(result["year"].iloc[:2]) == [2026, 2026]
    assert list(result["month"].iloc[:2]) == [1, 2]
    assert list(result["month_period"].iloc[:2]) == ["2026-01", "2026-02"]


def test_adds_day_of_week_and_day_of_month(transactions_frame):
    result = add_temporal_features(transactions_frame)

    # 2026-01-05 is a Monday.
    assert result["day_of_week"].iloc[0] == "Monday"
    assert result["day_of_month"].iloc[0] == 5


def test_missing_date_produces_missing_features_not_a_dropped_row(transactions_frame):
    result = add_temporal_features(transactions_frame)

    assert len(result) == len(transactions_frame)
    assert pd.isna(result["year"].iloc[2])
    assert pd.isna(result["month_period"].iloc[2])


def test_does_not_mutate_input(transactions_frame):
    original_columns = list(transactions_frame.columns)

    add_temporal_features(transactions_frame)

    assert list(transactions_frame.columns) == original_columns
