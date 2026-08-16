"""Deterministic recurring-transaction detection (PR-010).

A "recurring transaction" here means a merchant charge whose merchant,
amount and interval are all consistent enough over time to look like a
subscription or regular bill — not merely a merchant seen more than once.
See `detector.detect_recurring_transactions` for the entry point.
"""
