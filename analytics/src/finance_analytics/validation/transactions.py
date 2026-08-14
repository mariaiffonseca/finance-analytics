"""Schema validation for transaction data.

Independent of any notebook: it only inspects a `pandas.DataFrame` (typically
produced by `finance_analytics.io.csv.load_transactions_csv`) and reports
what it finds. It never mutates or drops rows, and never raises for data
issues — callers decide what to do with an invalid `ValidationResult`.
"""

from __future__ import annotations

from dataclasses import dataclass, field

import pandas as pd

from finance_analytics.data.schema import REQUIRED_COLUMNS, REQUIRED_VALUE_COLUMNS

#: Required-value columns whose invalidity is already captured by dedicated
#: date/amount checks, so they are skipped by the generic missing-value scan.
_COVERED_BY_TYPE_CHECK = {"date", "amount"}


@dataclass(frozen=True)
class ValidationResult:
    """Outcome of validating a transaction DataFrame's structure and values."""

    missing_columns: tuple[str, ...] = ()
    is_empty: bool = False
    invalid_date_rows: tuple[int, ...] = ()
    invalid_amount_rows: tuple[int, ...] = ()
    missing_value_rows: dict[str, tuple[int, ...]] = field(default_factory=dict)

    @property
    def has_structural_problems(self) -> bool:
        """Problems with the file itself rather than a specific row."""
        return bool(self.missing_columns) or self.is_empty

    @property
    def is_valid(self) -> bool:
        return not (
            self.has_structural_problems
            or self.invalid_date_rows
            or self.invalid_amount_rows
            or any(self.missing_value_rows.values())
        )


def validate_transactions(frame: pd.DataFrame) -> ValidationResult:
    """Validate a transaction DataFrame's structure and values."""
    missing_columns = tuple(column for column in REQUIRED_COLUMNS if column not in frame.columns)

    if missing_columns:
        # Row-level checks below assume the relevant columns exist.
        return ValidationResult(missing_columns=missing_columns, is_empty=frame.empty)

    invalid_date_rows = tuple(frame.index[frame["date"].isna()])
    invalid_amount_rows = tuple(frame.index[frame["amount"].isna()])

    missing_value_rows: dict[str, tuple[int, ...]] = {}
    for column in REQUIRED_VALUE_COLUMNS:
        if column in _COVERED_BY_TYPE_CHECK:
            continue
        values = frame[column]
        missing_mask = values.isna() | (values.astype(str).str.strip() == "")
        missing_rows = tuple(frame.index[missing_mask])
        if missing_rows:
            missing_value_rows[column] = missing_rows

    return ValidationResult(
        is_empty=frame.empty,
        invalid_date_rows=invalid_date_rows,
        invalid_amount_rows=invalid_amount_rows,
        missing_value_rows=missing_value_rows,
    )
