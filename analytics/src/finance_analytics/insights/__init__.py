"""Financial Behaviour & Insights Engine (PR-012).

Combines PR-009's anomaly results, PR-010's recurring-transaction results
and PR-011's merchant/category enrichment — plus two new period-comparison
rules of its own (spending trend, category change, income/expense change,
savings rate change) — into a small set of structured, deterministic
`Insight` objects. No ML, no LLM: every insight traces back to a documented
rule and the evidence that triggered it. See `engine.generate_insights` for
the entry point.
"""
