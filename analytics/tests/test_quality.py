from finance_analytics.data.quality import build_quality_report
from finance_analytics.io.csv import load_transactions_csv


def test_duplicate_rows_are_detected(fixtures_dir):
    frame = load_transactions_csv(fixtures_dir / "duplicates.csv")

    report = build_quality_report(frame)

    assert report.duplicate_row_count == 1


def test_duplicate_ids_are_detected(fixtures_dir):
    frame = load_transactions_csv(fixtures_dir / "duplicates.csv")

    report = build_quality_report(frame)

    assert report.duplicate_id_count == 1


def test_missing_values_are_counted(fixtures_dir):
    frame = load_transactions_csv(fixtures_dir / "duplicates.csv")

    report = build_quality_report(frame)

    # Row with id=3 has an empty amount value.
    assert report.missing_values_by_column["amount"] == 1
    assert report.invalid_amount_count == 1


def test_report_summarises_valid_dataset(fixtures_dir):
    frame = load_transactions_csv(fixtures_dir / "valid_transactions.csv")

    report = build_quality_report(frame)

    assert report.row_count == 4
    assert report.duplicate_row_count == 0
    assert report.duplicate_id_count == 0
    assert report.unique_merchant_count == 4
    assert report.unique_category_count == 4
    assert report.income_count == 1
    assert report.expense_count == 3
    assert report.date_range is not None


def test_report_renders_as_frame(fixtures_dir):
    frame = load_transactions_csv(fixtures_dir / "valid_transactions.csv")

    report = build_quality_report(frame)
    rendered = report.to_frame()

    assert rendered.loc["Rows", "value"] == 4
