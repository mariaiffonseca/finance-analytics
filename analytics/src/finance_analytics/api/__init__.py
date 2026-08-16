"""Analytics API (PR-013).

A FastAPI transport layer over the existing, already-validated analytics
engine (`finance_analytics.enrichment`, `.anomalies`, `.recurring`,
`.insights`). This package does not implement any analytical logic itself
— see `service.py`'s docstring for the orchestration boundary, and
`app.py` for the FastAPI application factory.
"""
