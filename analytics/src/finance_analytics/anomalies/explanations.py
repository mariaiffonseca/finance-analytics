"""Deterministic, template-based explanations for anomaly results.

No LLM is used to generate these (PR-009, Explainability — explicitly out of
scope for this PR). Every sentence is a fixed template filled in with the
same historical reference values used for scoring in `detector.py`, so the
same input always produces the same sentence, and the sentence always names
the baseline it was actually compared against.
"""

from __future__ import annotations

import math


def _ratio_phrase(amount: float, median: float) -> str | None:
    """`"3.8×"`-style phrase, or `None` when a ratio wouldn't be meaningful.

    `median == 0` and `NaN` medians have no meaningful ratio to report. A
    ratio that rounds to `"1.0×"` is also suppressed: it reads as "no
    different from typical", which would contradict a flagged anomaly — the
    MAD-based z-score and this median-based ratio measure different things
    and can disagree near the threshold, so the caller falls back to
    qualitative phrasing instead of asserting a number that looks unremarkable.
    """
    if median == 0 or math.isnan(median):
        return None
    text = f"{amount / median:.1f}×"
    return None if text == "1.0×" else text


def _explain(
    noun_phrase: str | None,
    amount: float,
    median: float,
    currency: str,
    is_anomaly: bool,
    is_unusually_low: bool,
) -> str:
    """Shared template logic behind every `explain_*_relative` function.

    `noun_phrase` is the merchant or category name the baseline is scoped
    to, or `None` for the global (unscoped) baseline.
    """
    subject = (
        f"your typical {noun_phrase} transaction" if noun_phrase else "your typical transaction"
    )
    history = f"{noun_phrase} history" if noun_phrase else "transaction history"
    ratio = _ratio_phrase(amount, median)

    if is_anomaly and ratio:
        return f"Amount is {ratio} higher than {subject} (usually around {median:.2f} {currency})."
    if is_anomaly:
        return f"Amount breaks from your otherwise consistent {history}."
    if is_unusually_low and ratio:
        return (
            f"Amount is {ratio} of {subject} (usually around {median:.2f} {currency}) — "
            "lower than usual, though only unusually large amounts are flagged."
        )
    if is_unusually_low:
        return (
            f"Amount is well below your otherwise consistent {history}, "
            "though only unusually large amounts are flagged."
        )
    return f"Amount is close to {subject} (usually around {median:.2f} {currency})."


def explain_merchant_relative(
    merchant: str,
    amount: float,
    median: float,
    is_anomaly: bool,
    currency: str = "EUR",
    is_unusually_low: bool = False,
) -> str:
    return _explain(merchant, amount, median, currency, is_anomaly, is_unusually_low)


def explain_category_relative(
    category: str,
    amount: float,
    median: float,
    is_anomaly: bool,
    currency: str = "EUR",
    is_unusually_low: bool = False,
) -> str:
    return _explain(category, amount, median, currency, is_anomaly, is_unusually_low)


def explain_global_relative(
    amount: float,
    median: float,
    is_anomaly: bool,
    currency: str = "EUR",
    is_unusually_low: bool = False,
) -> str:
    return _explain(None, amount, median, currency, is_anomaly, is_unusually_low)


def explain_insufficient_history(
    category_count: int, merchant_count: int, global_count: int
) -> str:
    return (
        "Not enough transaction history to evaluate this transaction "
        f"(seen this merchant {merchant_count}×, this category {category_count}×, "
        f"{global_count} prior transactions overall)."
    )
