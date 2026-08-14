import pandas as pd
import pytest

from finance_analytics.analysis.merchant import merchant_summary


@pytest.fixture
def transactions_frame() -> pd.DataFrame:
    return pd.DataFrame(
        {
            "amount": [-12.50, -13.00, -900.00, 2000.00],
            "merchant": ["Coffee Corner", "Coffee Corner", "MediaMarkt", "Employer Payroll"],
        }
    )


def test_income_rows_are_excluded(transactions_frame):
    summary = merchant_summary(transactions_frame)

    assert "Employer Payroll" not in set(summary["merchant"])


def test_aggregates_by_merchant(transactions_frame):
    summary = merchant_summary(transactions_frame)

    coffee = summary.set_index("merchant").loc["Coffee Corner"]
    assert coffee["transaction_count"] == 2
    assert coffee["total_spend"] == pytest.approx(25.50)
    assert coffee["mean_transaction"] == pytest.approx(12.75)


def test_sorted_by_total_spend_descending(transactions_frame):
    summary = merchant_summary(transactions_frame)

    assert next(iter(summary["merchant"])) == "MediaMarkt"
