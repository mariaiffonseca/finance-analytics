"""Merchant normalisation and transaction categorisation (PR-011).

The first reusable transaction-enrichment layer: raw merchant -> normalised
merchant -> category. Fully deterministic — no ML, no LLM, no fuzzy
matching. See `models.enrich_transactions` for the entry point.
"""
