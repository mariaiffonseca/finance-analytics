import pandas as pd

from finance_analytics.io.csv import load_transactions_csv


def test_valid_csv_loads_successfully(fixtures_dir):
    frame = load_transactions_csv(fixtures_dir / "valid_transactions.csv")

    assert len(frame) == 4
    assert list(frame.columns) == [
        "id",
        "date",
        "amount",
        "currency",
        "description",
        "merchant",
        "category",
        "account",
    ]


def test_dates_are_parsed_correctly(fixtures_dir):
    frame = load_transactions_csv(fixtures_dir / "valid_transactions.csv")

    assert pd.api.types.is_datetime64_any_dtype(frame["date"])
    assert frame["date"].iloc[0] == pd.Timestamp("2026-01-05")
    assert frame["date"].notna().all()


def test_malformed_amounts_are_detected(fixtures_dir):
    frame = load_transactions_csv(fixtures_dir / "malformed_values.csv")

    assert pd.api.types.is_float_dtype(frame["amount"])
    # Row with id=3 has amount "not-an-amount".
    invalid_row = frame.loc[frame["id"] == "3"].iloc[0]
    assert pd.isna(invalid_row["amount"])
    # Valid rows are unaffected.
    assert frame.loc[frame["id"] == "1"].iloc[0]["amount"] == -12.50


def test_malformed_dates_are_detected(fixtures_dir):
    frame = load_transactions_csv(fixtures_dir / "malformed_values.csv")

    # Row with id=2 has date "not-a-date".
    invalid_row = frame.loc[frame["id"] == "2"].iloc[0]
    assert pd.isna(invalid_row["date"])
    # Valid rows are unaffected.
    assert frame.loc[frame["id"] == "1"].iloc[0]["date"] == pd.Timestamp("2026-01-05")
