"""Category-level aggregation for transaction analysis.

Builds the category summary table used to investigate which categories
drive spending (PR-008, RQ2) and as a foundation for later feature
engineering. Restricted to expense transactions (amount < 0): categories
are compared by how much they cost, not how much money moves through them,
so an `Income` category — which only ever has positive amounts — has
nothing to report here and is absent from the result.
"""

from __future__ import annotations

import pandas as pd


def category_summary(
    frame: pd.DataFrame,
    amount_column: str = "amount",
    category_column: str = "category",
    date_column: str = "date",
) -> pd.DataFrame:
    """Build a category-level analytical table.

    Columns: `category`, `transaction_count`, `total_spend`,
    `mean_transaction`, `median_transaction`, `share_of_spend`,
    `monthly_volatility`.

    `total_spend`/`mean_transaction`/`median_transaction` are reported as
    positive magnitudes. `monthly_volatility` is the coefficient of
    variation (std / mean) of the category's total spend across the months
    present in `frame`; a category observed in only one month has nothing
    to vary against and gets `NaN`.
    """
    expenses = (
        frame[frame[amount_column] < 0].dropna(subset=[amount_column, category_column]).copy()
    )
    expenses["spend"] = expenses[amount_column].abs()

    grand_total = expenses["spend"].sum()

    monthly_totals = (
        expenses.assign(month_period=expenses[date_column].dt.to_period("M"))
        .groupby([category_column, "month_period"])["spend"]
        .sum()
    )
    monthly_volatility = monthly_totals.groupby(level=category_column).agg(
        lambda totals: totals.std() / totals.mean()
    )

    summary = expenses.groupby(category_column)["spend"].agg(
        transaction_count="count",
        total_spend="sum",
        mean_transaction="mean",
        median_transaction="median",
    )
    summary["share_of_spend"] = (
        summary["total_spend"] / grand_total if grand_total else float("nan")
    )
    summary["monthly_volatility"] = monthly_volatility

    return summary.reset_index().sort_values("total_spend", ascending=False).reset_index(drop=True)
