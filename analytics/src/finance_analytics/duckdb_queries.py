"""DuckDB foundation for querying transaction data with SQL.

Keeps SQL separate from notebooks: query text lives here, notebooks only
call functions and display results. This is a thin querying layer, not a
data warehouse — one in-memory connection, one registered view.
"""

from __future__ import annotations

import duckdb
import pandas as pd

#: Name transactions are queryable under once registered.
TABLE_NAME = "transactions"


def connect_with_transactions(frame: pd.DataFrame) -> duckdb.DuckDBPyConnection:
    """Create an in-memory DuckDB connection with `frame` queryable as `transactions`."""
    connection = duckdb.connect(database=":memory:")
    connection.register(TABLE_NAME, frame)
    return connection


def total_transaction_count(connection: duckdb.DuckDBPyConnection) -> int:
    return connection.execute(f"SELECT COUNT(*) FROM {TABLE_NAME}").fetchone()[0]


def total_income(connection: duckdb.DuckDBPyConnection) -> float:
    result = connection.execute(
        f"SELECT COALESCE(SUM(amount), 0) FROM {TABLE_NAME} WHERE amount > 0"
    ).fetchone()[0]
    return float(result)


def total_expenses(connection: duckdb.DuckDBPyConnection) -> float:
    result = connection.execute(
        f"SELECT COALESCE(SUM(amount), 0) FROM {TABLE_NAME} WHERE amount < 0"
    ).fetchone()[0]
    return float(result)


def transactions_by_category(connection: duckdb.DuckDBPyConnection) -> pd.DataFrame:
    return connection.execute(
        f"""
        SELECT
            category,
            COUNT(*) AS transaction_count,
            SUM(amount) AS total_amount
        FROM {TABLE_NAME}
        GROUP BY category
        ORDER BY transaction_count DESC
        """
    ).df()


def transactions_by_month(connection: duckdb.DuckDBPyConnection) -> pd.DataFrame:
    return connection.execute(
        f"""
        SELECT
            strftime(date, '%Y-%m') AS month,
            COUNT(*) AS transaction_count,
            SUM(amount) AS total_amount
        FROM {TABLE_NAME}
        WHERE date IS NOT NULL
        GROUP BY month
        ORDER BY month
        """
    ).df()
