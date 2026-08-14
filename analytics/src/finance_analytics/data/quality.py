"""Data-quality reporting for transaction datasets.

This is a data-quality report, not an analytics report: it describes the
shape and integrity of the dataset (missing values, duplicates, malformed
values), not spending behaviour or insights.
"""

from __future__ import annotations

from dataclasses import dataclass

import pandas as pd


@dataclass(frozen=True)
class DataQualityReport:
    """A snapshot of a transaction dataset's structural health."""

    row_count: int
    column_count: int
    missing_values_by_column: dict[str, int]
    duplicate_row_count: int
    duplicate_id_count: int
    invalid_date_count: int
    invalid_amount_count: int
    unique_merchant_count: int
    unique_category_count: int
    date_range: tuple[pd.Timestamp, pd.Timestamp] | None
    income_count: int
    expense_count: int

    def to_frame(self) -> pd.DataFrame:
        """Render the report as a single-column DataFrame for display."""
        date_range_label = "n/a"
        if self.date_range is not None:
            start, end = self.date_range
            date_range_label = f"{start.date()} to {end.date()}"

        rows: dict[str, object] = {
            "Rows": self.row_count,
            "Columns": self.column_count,
            "Duplicate rows": self.duplicate_row_count,
            "Duplicate transaction IDs": self.duplicate_id_count,
            "Invalid dates": self.invalid_date_count,
            "Invalid amounts": self.invalid_amount_count,
            "Unique merchants": self.unique_merchant_count,
            "Unique categories": self.unique_category_count,
            "Date range": date_range_label,
            "Income transactions": self.income_count,
            "Expense transactions": self.expense_count,
        }
        for column, count in self.missing_values_by_column.items():
            rows[f"Missing values — {column}"] = count

        return pd.DataFrame.from_dict(rows, orient="index", columns=["value"])


def build_quality_report(frame: pd.DataFrame) -> DataQualityReport:
    """Build a data-quality report for a transaction DataFrame.

    Assumes `frame` has the columns produced by
    `finance_analytics.io.csv.load_transactions_csv` (missing columns are a
    schema-validation concern, not a data-quality one); columns that are
    nonetheless absent are reported as zero/empty rather than raising.
    """
    missing_values_by_column = {column: int(frame[column].isna().sum()) for column in frame.columns}

    duplicate_row_count = int(frame.duplicated().sum())
    duplicate_id_count = int(frame["id"].duplicated().sum()) if "id" in frame.columns else 0

    invalid_date_count = int(frame["date"].isna().sum()) if "date" in frame.columns else 0
    invalid_amount_count = int(frame["amount"].isna().sum()) if "amount" in frame.columns else 0

    unique_merchant_count = (
        int(frame["merchant"].nunique(dropna=True)) if "merchant" in frame.columns else 0
    )
    unique_category_count = (
        int(frame["category"].nunique(dropna=True)) if "category" in frame.columns else 0
    )

    date_range = None
    if "date" in frame.columns:
        valid_dates = frame["date"].dropna()
        if not valid_dates.empty:
            date_range = (valid_dates.min(), valid_dates.max())

    income_count = 0
    expense_count = 0
    if "amount" in frame.columns:
        valid_amounts = frame["amount"].dropna()
        income_count = int((valid_amounts > 0).sum())
        expense_count = int((valid_amounts < 0).sum())

    return DataQualityReport(
        row_count=len(frame),
        column_count=len(frame.columns),
        missing_values_by_column=missing_values_by_column,
        duplicate_row_count=duplicate_row_count,
        duplicate_id_count=duplicate_id_count,
        invalid_date_count=invalid_date_count,
        invalid_amount_count=invalid_amount_count,
        unique_merchant_count=unique_merchant_count,
        unique_category_count=unique_category_count,
        date_range=date_range,
        income_count=income_count,
        expense_count=expense_count,
    )
