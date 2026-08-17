"""`POST /analytics/analyse` (PR-013 §12-13).

Covers request validation, error-response shape, and a real end-to-end
contract test that exercises the actual analytics engine (not a mock) —
PR-013 §13: "Do not rely exclusively on mocked analytics components."
"""

from __future__ import annotations

from unittest.mock import patch

import pandas as pd
from fastapi.testclient import TestClient

from finance_analytics.anomalies.detector import detect_anomalies
from finance_analytics.insights.engine import generate_insights
from finance_analytics.io.csv import load_transactions_csv
from finance_analytics.recurring.detector import detect_recurring_transactions

FIXTURE_PATH = "data/raw/finance_analytics_test_transactions.csv"


def make_transaction(
    id_: str,
    date: str,
    amount: float,
    *,
    currency: str = "EUR",
    description: str = "",
    merchant: str = "Merchant",
    category: str = "Shopping",
    account: str = "Main Account",
) -> dict:
    return {
        "id": id_,
        "date": date,
        "amount": amount,
        "currency": currency,
        "description": description,
        "merchant": merchant,
        "category": category,
        "account": account,
    }


def _clean_fixture() -> pd.DataFrame:
    """Same three-step cleaning `test_insights_engine.py::_clean_fixture`
    applies: drop structurally invalid rows, the duplicate id, and the
    QA-only row disguised as a transaction."""
    raw = load_transactions_csv(FIXTURE_PATH)
    clean = raw.dropna(subset=["date", "amount"]).drop_duplicates(subset=["id"])
    return clean[~clean["description"].fillna("").str.contains("test", case=False)]


def _frame_to_payload(frame: pd.DataFrame) -> list[dict]:
    records = frame.to_dict(orient="records")
    for record in records:
        record["id"] = str(record["id"])
        record["date"] = record["date"].date().isoformat()
        for column in ("description", "category", "account"):
            if pd.isna(record.get(column)):
                record[column] = None
    return records


# --- Validation ---------------------------------------------------------


def test_valid_request_is_accepted(client: TestClient) -> None:
    payload = {"transactions": [make_transaction("1", "2026-01-05", -12.50)]}

    response = client.post("/analytics/analyse", json=payload)

    assert response.status_code == 200


def test_missing_required_field_is_rejected(client: TestClient) -> None:
    transaction = make_transaction("1", "2026-01-05", -12.50)
    del transaction["merchant"]

    response = client.post("/analytics/analyse", json={"transactions": [transaction]})

    assert response.status_code == 422


def test_invalid_date_is_rejected(client: TestClient) -> None:
    payload = {"transactions": [make_transaction("1", "not-a-date", -12.50)]}

    response = client.post("/analytics/analyse", json=payload)

    assert response.status_code == 422


def test_invalid_amount_type_is_rejected(client: TestClient) -> None:
    transaction = make_transaction("1", "2026-01-05", -12.50)
    transaction["amount"] = "not-a-number"

    response = client.post("/analytics/analyse", json={"transactions": [transaction]})

    assert response.status_code == 422


def test_empty_transaction_list_is_rejected(client: TestClient) -> None:
    response = client.post("/analytics/analyse", json={"transactions": []})

    assert response.status_code == 422


def test_blank_merchant_is_rejected(client: TestClient) -> None:
    transaction = make_transaction("1", "2026-01-05", -12.50)
    transaction["merchant"] = "   "

    response = client.post("/analytics/analyse", json={"transactions": [transaction]})

    assert response.status_code == 422


def test_nan_amount_passes_schema_but_fails_structural_validation(client: TestClient) -> None:
    """`amount: NaN` is a value pydantic's `float` field accepts (JSON's
    non-standard NaN extension), but it is meaningless for analytics.
    `service.run_analysis` re-validates with
    `finance_analytics.validation.transactions.validate_transactions` and
    rejects it as a 400 structural error rather than letting it flow into
    the analytics engine (PR-013 §5 note in `service.py`)."""
    raw_body = (
        b'{"transactions": [{"id": "1", "date": "2026-01-05", "amount": NaN, '
        b'"currency": "EUR", "merchant": "X"}]}'
    )

    response = client.post(
        "/analytics/analyse", content=raw_body, headers={"content-type": "application/json"}
    )

    assert response.status_code == 400
    assert "detail" in response.json()


def test_duplicate_transaction_ids_are_deduplicated(client: TestClient) -> None:
    duplicated = make_transaction("1", "2026-01-05", -12.50)
    payload = {"transactions": [duplicated, dict(duplicated)]}

    response = client.post("/analytics/analyse", json=payload)

    body = response.json()
    assert response.status_code == 200
    assert body["summary"]["transaction_count"] == 1
    assert body["metadata"]["requested_transaction_count"] == 2
    assert body["metadata"]["processed_transaction_count"] == 1
    assert body["metadata"]["duplicate_id_count"] == 1
    assert body["metadata"]["duplicate_row_count"] == 1


# --- Error responses do not leak internals --------------------------------


def test_analytics_failure_returns_generic_500_without_internal_details() -> None:
    from finance_analytics.api.app import app

    client = TestClient(app, raise_server_exceptions=False)
    payload = {"transactions": [make_transaction("1", "2026-01-05", -12.50)]}

    with patch(
        "finance_analytics.api.routes.analytics.run_analysis",
        side_effect=RuntimeError("some internal pandas detail"),
    ):
        response = client.post("/analytics/analyse", json=payload)

    assert response.status_code == 500
    assert response.json() == {"detail": "Internal analytics failure."}
    assert "some internal pandas detail" not in response.text
    assert "Traceback" not in response.text


# --- Response shape --------------------------------------------------------


def test_response_contains_expected_top_level_sections(client: TestClient) -> None:
    payload = {"transactions": [make_transaction("1", "2026-01-05", -12.50)]}

    body = client.post("/analytics/analyse", json=payload).json()

    assert set(body.keys()) == {"summary", "insights", "anomalies", "recurring", "metadata"}


def test_summary_reflects_income_and_expenses(client: TestClient) -> None:
    payload = {
        "transactions": [
            make_transaction("1", "2026-01-05", -40.0, category="Groceries"),
            make_transaction("2", "2026-01-10", 1000.0, category="Income"),
        ]
    }

    body = client.post("/analytics/analyse", json=payload).json()

    assert body["summary"]["total_expenses"] == -40.0
    assert body["summary"]["total_income"] == 1000.0
    assert body["summary"]["net_savings"] == 960.0
    assert body["summary"]["income_count"] == 1
    assert body["summary"]["expense_count"] == 1


# --- Real end-to-end contract test (PR-013 §13) -----------------------------


def test_analyse_matches_the_real_analytics_engine_on_the_project_fixture(
    client: TestClient,
) -> None:
    """Feeds the project's own cleaned test fixture through the HTTP API
    and asserts the response carries the *same* insight/anomaly/recurring
    identifiers as calling `finance_analytics.insights.engine.generate_insights`,
    `.anomalies.detector.detect_anomalies` and
    `.recurring.detector.detect_recurring_transactions` directly on the same
    DataFrame. This is a real contract test: the API is not mocked, and the
    fixture is the same deterministic dataset the rest of the analytics
    test suite already validates against."""
    clean = _clean_fixture()
    payload = {"transactions": _frame_to_payload(clean)}

    response = client.post("/analytics/analyse", json=payload)

    assert response.status_code == 200
    body = response.json()

    expected_insights = generate_insights(clean)
    expected_anomalies = [result for result in detect_anomalies(clean) if result.is_anomaly]
    expected_recurring = [
        result
        for result in detect_recurring_transactions(clean)
        if result.classification in {"Recurring", "Possible recurring"}
    ]

    assert [insight["id"] for insight in body["insights"]] == [i.id for i in expected_insights]
    assert [a["transaction_id"] for a in body["anomalies"]] == [
        r.transaction_id for r in expected_anomalies
    ]
    assert [(r["merchant"], r["currency"]) for r in body["recurring"]] == [
        (r.merchant, r.currency) for r in expected_recurring
    ]
    assert body["summary"]["transaction_count"] == len(clean)
    assert len(expected_insights) > 0, "fixture should produce at least one insight"
