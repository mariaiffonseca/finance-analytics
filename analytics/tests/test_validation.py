import pandas as pd

from finance_analytics.io.csv import load_transactions_csv
from finance_analytics.validation.transactions import validate_transactions


def test_missing_required_columns_are_detected(fixtures_dir):
    frame = load_transactions_csv(fixtures_dir / "missing_column.csv")

    result = validate_transactions(frame)

    assert result.missing_columns == ("category",)
    assert result.has_structural_problems
    assert not result.is_valid


def test_required_fields_are_checked(fixtures_dir):
    frame = load_transactions_csv(fixtures_dir / "missing_values.csv")

    result = validate_transactions(frame)

    assert "merchant" in result.missing_value_rows
    assert result.missing_value_rows["merchant"] == (1,)
    assert not result.is_valid


def test_invalid_dates_and_amounts_are_reported(fixtures_dir):
    frame = load_transactions_csv(fixtures_dir / "malformed_values.csv")

    result = validate_transactions(frame)

    assert result.invalid_date_rows == (1,)
    assert result.invalid_amount_rows == (2,)
    assert not result.is_valid


def test_structural_problems_are_handled_for_empty_dataset():
    empty_frame = pd.DataFrame(
        columns=[
            "id",
            "date",
            "amount",
            "currency",
            "description",
            "merchant",
            "category",
            "account",
        ]
    )

    result = validate_transactions(empty_frame)

    assert result.is_empty
    assert result.has_structural_problems
    assert not result.is_valid


def test_valid_dataset_passes_validation(fixtures_dir):
    frame = load_transactions_csv(fixtures_dir / "valid_transactions.csv")

    result = validate_transactions(frame)

    assert result.is_valid
    assert not result.has_structural_problems
