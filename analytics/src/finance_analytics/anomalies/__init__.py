"""Transaction anomaly detection (PR-009).

An "anomaly" here means a transaction whose amount is unusually different
from the user's own historical behaviour — not fraud, not incorrect
spending, not financial advice. See `detector.detect_anomalies` for the
entry point.
"""
