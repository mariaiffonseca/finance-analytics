"""Merchant-level aggregation for transaction analysis.

Builds the merchant summary table used to investigate which merchants
dominate spending (PR-008, RQ4): total spend, transaction count and average
transaction size, so a merchant with many small transactions can be told
apart from one with a few large ones. Restricted to expense transactions —
see `finance_analytics.analysis.category` for why.
"""

from __future__ import annotations

import pandas as pd


def merchant_summary(
    frame: pd.DataFrame,
    amount_column: str = "amount",
    merchant_column: str = "merchant",
) -> pd.DataFrame:
    """Build a merchant-level analytical table.

    Columns: `merchant`, `transaction_count`, `total_spend`,
    `mean_transaction`, `median_transaction` — all reported as positive
    magnitudes, sorted by `total_spend` descending.
    """
    expenses = (
        frame[frame[amount_column] < 0].dropna(subset=[amount_column, merchant_column]).copy()
    )
    expenses["spend"] = expenses[amount_column].abs()

    summary = expenses.groupby(merchant_column)["spend"].agg(
        transaction_count="count",
        total_spend="sum",
        mean_transaction="mean",
        median_transaction="median",
    )

    return summary.reset_index().sort_values("total_spend", ascending=False).reset_index(drop=True)
