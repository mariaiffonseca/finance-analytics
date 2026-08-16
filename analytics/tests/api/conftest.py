from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from finance_analytics.api.app import app


@pytest.fixture
def client() -> TestClient:
    return TestClient(app)
