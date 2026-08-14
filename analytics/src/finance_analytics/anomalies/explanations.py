"""Deterministic, template-based explanations for anomaly results.

No LLM is used to generate these (PR-009, Explainability — explicitly out of
scope for this PR). Every sentence is a fixed template filled in with the
same historical reference values used for scoring in `detector.py`, so the
same input always produces the same sentence, and the sentence always names
the baseline it was actually compared against.
"""

from __future__ import annotations


def _ratio_phrase(amount: float, median: float) -> str | None:
    """`"3.8×"`-style phrase, or `None` when a ratio wouldn't be meaningful."""
    if not median:
        return None
    return f"{amount / median:.1f}×"


def explain_merchant_relative(merchant: str, amount: float, median: float, is_anomaly: bool) -> str:
    ratio = _ratio_phrase(amount, median)
    if is_anomaly and ratio:
        return (
            f"Amount is {ratio} higher than your typical {merchant} transaction "
            f"(usually around €{median:.2f})."
        )
    if is_anomaly:
        return f"Amount breaks from your otherwise consistent {merchant} history."
    return f"Amount is close to your typical {merchant} transaction (usually around €{median:.2f})."


def explain_category_relative(category: str, amount: float, median: float, is_anomaly: bool) -> str:
    ratio = _ratio_phrase(amount, median)
    if is_anomaly and ratio:
        return (
            f"Amount is {ratio} higher than your typical {category} transaction "
            f"(usually around €{median:.2f})."
        )
    if is_anomaly:
        return f"Amount breaks from your otherwise consistent {category} history."
    return f"Amount is close to your typical {category} transaction (usually around €{median:.2f})."


def explain_global_relative(amount: float, median: float, is_anomaly: bool) -> str:
    ratio = _ratio_phrase(amount, median)
    if is_anomaly and ratio:
        return f"Amount is {ratio} higher than your typical transaction (usually around €{median:.2f})."
    if is_anomaly:
        return "Amount breaks from your otherwise consistent transaction history."
    return "Amount is close to your typical transaction range."


def explain_insufficient_history(
    category_count: int, merchant_count: int, global_count: int
) -> str:
    return (
        "Not enough transaction history to evaluate this transaction "
        f"(seen this merchant {merchant_count}×, this category {category_count}×, "
        f"{global_count} prior transactions overall)."
    )
