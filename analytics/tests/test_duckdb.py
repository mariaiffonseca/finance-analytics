import pandas as pd
import pytest

from finance_analytics.duckdb_queries import (
    connect_with_transactions,
    total_expenses,
    total_income,
    total_transaction_count,
    transactions_by_category,
    transactions_by_month,
)


@pytest.fixture
def transactions_frame() -> pd.DataFrame:
    return pd.DataFrame(
        {
            "id": ["1", "2", "3", "4"],
            "date": pd.to_datetime(["2026-01-05", "2026-01-06", "2026-01-10", "2026-02-01"]),
            "amount": [-12.50, -40.00, 2000.00, -15.99],
            "currency": ["EUR"] * 4,
            "description": ["Coffee", "Groceries", "Salary", "Subscription"],
            "merchant": ["Coffee Corner", "Continente", "Employer Payroll", "Netflix"],
            "category": ["Food & Dining", "Groceries", "Income", "Subscriptions"],
            "account": ["Main Account"] * 4,
        }
    )


def test_dataframe_can_be_queried(transactions_frame):
    connection = connect_with_transactions(transactions_frame)

    result = connection.execute("SELECT COUNT(*) FROM transactions").fetchone()[0]

    assert result == 4


def test_total_transaction_count(transactions_frame):
    connection = connect_with_transactions(transactions_frame)

    assert total_transaction_count(connection) == 4


def test_total_income_and_expenses(transactions_frame):
    connection = connect_with_transactions(transactions_frame)

    assert total_income(connection) == pytest.approx(2000.00)
    assert total_expenses(connection) == pytest.approx(-68.49)


def test_transactions_by_category(transactions_frame):
    connection = connect_with_transactions(transactions_frame)

    result = transactions_by_category(connection)

    assert set(result["category"]) == {"Food & Dining", "Groceries", "Income", "Subscriptions"}
    assert result["transaction_count"].sum() == 4


def test_transactions_by_month(transactions_frame):
    connection = connect_with_transactions(transactions_frame)

    result = transactions_by_month(connection)

    assert list(result["month"]) == ["2026-01", "2026-02"]
    assert list(result["transaction_count"]) == [3, 1]
