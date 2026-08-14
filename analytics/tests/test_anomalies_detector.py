import pandas as pd

from finance_analytics.anomalies.detector import (
    ANOMALY_Z_THRESHOLD,
    AnomalyResult,
    detect_anomalies,
)


def _expense_row(id_, date, amount, category="Groceries", merchant="Continente"):
    return {
        "id": id_,
        "date": pd.Timestamp(date),
        "amount": amount,
        "category": category,
        "merchant": merchant,
    }


def _frame(rows: list[dict]) -> pd.DataFrame:
    return pd.DataFrame(rows)


def _result_for(results: list[AnomalyResult], transaction_id: str) -> AnomalyResult:
    return next(r for r in results if r.transaction_id == transaction_id)


def test_insufficient_history_for_the_very_first_transaction():
    frame = _frame([_expense_row("1", "2026-01-01", -12.0)])

    result = _result_for(detect_anomalies(frame), "1")

    assert result.method == "insufficient_history"
    assert result.anomaly_score is None
    assert result.is_anomaly is False


def test_normal_transaction_close_to_category_history_is_not_flagged():
    # 5 prior Groceries transactions around ~11 (distinct merchants, so the
    # merchant tier never has enough history to take priority), then a 6th
    # at 12 from a merchant never seen before — unremarkable for the category.
    rows = [
        _expense_row(str(i), f"2026-01-0{i}", amount, merchant=f"Shop{i}")
        for i, amount in enumerate([-10.0, -11.0, -12.0, -10.0, -11.0], start=1)
    ]
    rows.append(_expense_row("6", "2026-01-06", -12.0, merchant="ShopX"))
    frame = _frame(rows)

    result = _result_for(detect_anomalies(frame), "6")

    assert result.method == "category_relative_robust_z"
    assert result.is_anomaly is False


def test_large_transaction_is_flagged_via_global_baseline_when_no_category_history():
    # 5 prior small transactions across categories with no history of their
    # own yet, then a single one-off "Travel" purchase far above them all.
    rows = [
        _expense_row("1", "2026-01-01", -10.0, category="Groceries", merchant="Continente"),
        _expense_row("2", "2026-01-02", -12.0, category="Food & Dining", merchant="Cafe"),
        _expense_row("3", "2026-01-03", -9.0, category="Transport", merchant="CP"),
        _expense_row("4", "2026-01-04", -11.0, category="Health", merchant="Pharmacy"),
        _expense_row("5", "2026-01-05", -10.0, category="Utilities", merchant="EDP"),
        _expense_row("6", "2026-01-06", -650.0, category="Travel", merchant="TAP Air"),
    ]
    frame = _frame(rows)

    result = _result_for(detect_anomalies(frame), "6")

    assert result.method == "global_relative_robust_z"
    assert result.is_anomaly is True
    assert result.anomaly_score > ANOMALY_Z_THRESHOLD


def test_category_specific_large_transaction_is_not_flagged():
    # Travel history is itself large (500-700 range) — a further large
    # Travel purchase is normal *for this category*, not an anomaly.
    rows = [
        _expense_row("1", "2026-01-01", -500.0, category="Travel", merchant="TAP Air"),
        _expense_row("2", "2026-01-08", -650.0, category="Travel", merchant="Ryanair"),
        _expense_row("3", "2026-01-15", -600.0, category="Travel", merchant="TAP Air"),
        _expense_row("4", "2026-01-22", -620.0, category="Travel", merchant="Ryanair"),
    ]
    frame = _frame(rows)

    result = _result_for(detect_anomalies(frame), "4")

    assert result.method == "category_relative_robust_z"
    assert result.is_anomaly is False


def test_merchant_specific_deviation_is_flagged_even_with_thin_category_history():
    # Spotify charges an identical amount every time (a tight, trustworthy
    # merchant baseline from just 2 observations) even though Subscriptions
    # as a category never reaches the category minimum-history threshold.
    rows = [
        _expense_row("1", "2026-01-05", -29.99, category="Subscriptions", merchant="Spotify"),
        _expense_row("2", "2026-02-05", -29.99, category="Subscriptions", merchant="Spotify"),
        _expense_row("3", "2026-03-05", -59.99, category="Subscriptions", merchant="Spotify"),
    ]
    frame = _frame(rows)

    result = _result_for(detect_anomalies(frame), "3")

    assert result.method == "merchant_relative_robust_z"
    assert result.is_anomaly is True
    assert result.reference_context["merchant_transaction_count"] == 2


def test_income_rows_are_not_scored():
    frame = _frame(
        [
            _expense_row("1", "2026-01-01", -12.0),
            {
                "id": "2",
                "date": pd.Timestamp("2026-01-02"),
                "amount": 3000.0,
                "category": "Income",
                "merchant": "Employer",
            },
        ]
    )

    results = detect_anomalies(frame)

    assert {r.transaction_id for r in results} == {"1"}


def test_scores_are_deterministic():
    rows = [
        _expense_row(str(i), f"2026-01-0{i}", amount)
        for i, amount in enumerate([-10.0, -11.0, -12.0, -10.0, -50.0], start=1)
    ]
    frame = _frame(rows)

    first_run = [r.anomaly_score for r in detect_anomalies(frame)]
    second_run = [r.anomaly_score for r in detect_anomalies(frame)]

    assert first_run == second_run


def test_larger_deviation_scores_higher():
    # Same history (distinct merchants, so the category tier is used), two
    # candidate transactions compared independently against that identical
    # baseline — a moderate deviation should score lower than a large one.
    history = [
        _expense_row(str(i), f"2026-01-0{i}", amount, merchant=f"Shop{i}")
        for i, amount in enumerate([-10.0, -11.0, -9.0, -12.0, -10.0], start=1)
    ]
    moderate_frame = _frame([*history, _expense_row("6", "2026-01-06", -15.0, merchant="ShopX")])
    large_frame = _frame([*history, _expense_row("6", "2026-01-06", -40.0, merchant="ShopX")])

    moderate = _result_for(detect_anomalies(moderate_frame), "6")
    large = _result_for(detect_anomalies(large_frame), "6")

    assert moderate.method == "category_relative_robust_z"
    assert large.method == "category_relative_robust_z"
    assert large.anomaly_score > moderate.anomaly_score
