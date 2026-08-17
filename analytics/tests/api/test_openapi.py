"""Verifies the generated OpenAPI documentation (PR-013 §11)."""

from __future__ import annotations

from fastapi.testclient import TestClient


def test_docs_ui_is_served(client: TestClient) -> None:
    response = client.get("/docs")

    assert response.status_code == 200


def test_openapi_json_is_served(client: TestClient) -> None:
    response = client.get("/openapi.json")

    assert response.status_code == 200


def test_openapi_declares_both_endpoints(client: TestClient) -> None:
    schema = client.get("/openapi.json").json()

    assert "/health" in schema["paths"]
    assert "get" in schema["paths"]["/health"]
    assert "/analytics/analyse" in schema["paths"]
    assert "post" in schema["paths"]["/analytics/analyse"]


def test_openapi_analyse_endpoint_has_a_description() -> None:
    from finance_analytics.api.app import app

    schema = app.openapi()
    operation = schema["paths"]["/analytics/analyse"]["post"]

    assert operation.get("summary")
    assert operation.get("description")


def test_openapi_declares_request_and_response_schemas(client: TestClient) -> None:
    schema = client.get("/openapi.json").json()
    components = schema["components"]["schemas"]

    assert "AnalysisRequest" in components
    assert "AnalysisResponse" in components
    assert "TransactionRequest" in components


def test_openapi_declares_error_responses(client: TestClient) -> None:
    schema = client.get("/openapi.json").json()
    responses = schema["paths"]["/analytics/analyse"]["post"]["responses"]

    assert "400" in responses
    assert "422" in responses
    assert "500" in responses
