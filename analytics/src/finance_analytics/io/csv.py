"""Pandas CSV loading for transaction data.

Loading is kept separate from validation: this module only turns a CSV file
into a DataFrame with the expected dtypes. It does not judge whether the data
is valid — that is `finance_analytics.validation`'s job.
"""

from __future__ import annotations

from pathlib import Path

import pandas as pd

#: Strict ISO-8601 date format used by the transaction CSV export.
DATE_FORMAT = "%Y-%m-%d"


def load_transactions_csv(path: str | Path) -> pd.DataFrame:
    """Load a transaction CSV into a DataFrame.

    Every column is read as a string first so pandas never guesses a dtype
    and silently turns a malformed value into a different, seemingly-valid
    one (e.g. inferring numeric dtype and turning "not-an-amount" into 0).
    `date` and `amount` are then explicitly parsed, when present:

    - `date` is parsed against `DATE_FORMAT`; values that do not match become
      `NaT` rather than a guessed or misparsed date.
    - `amount` is parsed as float64; values that are not a plain decimal
      number become `NaN` rather than being truncated or zeroed. Transaction
      amounts have at most two decimal places, which float64 represents
      exactly, so no fixed-point/Decimal type is needed for this analytics
      workspace.

    Rows with `NaT`/`NaN`, or a dataset missing `date`/`amount` altogether,
    are not dropped or rejected here — detecting and reporting them is the
    responsibility of `finance_analytics.validation` and
    `finance_analytics.data.quality`, so a caller always gets a full picture
    of the file rather than a partial one silently filtered by the loader.

    A malformed/unreadable CSV file (wrong delimiter, corrupt structure)
    raises the underlying `pandas` parser error — that is a hard failure,
    not a row-level data-quality issue.
    """
    frame = pd.read_csv(path, dtype=str, keep_default_na=False, na_values=[""])

    if "date" in frame.columns:
        frame["date"] = pd.to_datetime(frame["date"], format=DATE_FORMAT, errors="coerce")

    if "amount" in frame.columns:
        frame["amount"] = pd.to_numeric(frame["amount"], errors="coerce")

    return frame
