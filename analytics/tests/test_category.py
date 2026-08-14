import pandas as pd
import pytest

from finance_analytics.analysis.category import category_summary


@pytest.fixture
def transactions_frame() -> pd.DataFrame:
    return pd.DataFrame(
        {
            "date": pd.to_datetime(
                ["2026-01-05", "2026-02-05", "2026-01-10", "2026-01-31", "2026-01-15"]
            ),
            "amount": [-50.00, -30.00, -10.00, -10.00, 2000.00],
            "category": ["Groceries", "Groceries", "Transport", "Transport", "Income"],
        }
    )


def test_income_category_is_excluded(transactions_frame):
    summary = category_summary(transactions_frame)

    assert "Income" not in set(summary["category"])


def test_total_spend_is_a_positive_magnitude(transactions_frame):
    summary = category_summary(transactions_frame)

    groceries = summary.set_index("category").loc["Groceries"]
    assert groceries["total_spend"] == pytest.approx(80.00)
    assert groceries["transaction_count"] == 2
    assert groceries["mean_transaction"] == pytest.approx(40.00)
    assert groceries["median_transaction"] == pytest.approx(40.00)


def test_share_of_spend_sums_to_one(transactions_frame):
    summary = category_summary(transactions_frame)

    assert summary["share_of_spend"].sum() == pytest.approx(1.0)


def test_monthly_volatility_is_nan_for_single_month_category(transactions_frame):
    summary = category_summary(transactions_frame)

    # Transport only appears in January — nothing to compare across months.
    transport = summary.set_index("category").loc["Transport"]
    assert pd.isna(transport["monthly_volatility"])


def test_monthly_volatility_is_defined_for_multi_month_category(transactions_frame):
    summary = category_summary(transactions_frame)

    # Groceries appears in January (50.00) and February (30.00).
    groceries = summary.set_index("category").loc["Groceries"]
    assert not pd.isna(groceries["monthly_volatility"])
