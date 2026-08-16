"""Deterministic, template-based explanations for recurring-transaction
results.

No LLM is used to generate these (PR-010, Explainability — explicit
requirement). Every sentence is a fixed template filled in with the same
statistics used for classification in `detector.py`, so the same input
always produces the same sentence.
"""

from __future__ import annotations

import math


def _format_amount(amount: float, currency: str) -> str:
    return f"{amount:.2f} {currency}"


def explain_recurring(
    merchant: str,
    occurrences: int,
    median_interval_days: float,
    frequency: str,
    median_amount: float,
    currency: str,
    amount_variation: float,
) -> str:
    amount_phrase = (
        f"a consistent amount of {_format_amount(median_amount, currency)}"
        if amount_variation <= 0.05
        else f"a similar amount, around {_format_amount(median_amount, currency)}"
    )
    interval_phrase = (
        f"a stable {frequency.lower()} interval"
        if frequency != "Other regular interval"
        else f"a stable ~{median_interval_days:.0f}-day interval"
    )
    return (
        f"{merchant} appears {occurrences} times with {interval_phrase} "
        f"(~{median_interval_days:.0f} days) and {amount_phrase}."
    )


def explain_possible_recurring(
    merchant: str,
    occurrences: int,
    median_interval_days: float,
    median_amount: float,
    currency: str,
    min_occurrences_for_recurring: int,
) -> str:
    amount_phrase = f"around {_format_amount(median_amount, currency)}"
    if occurrences < min_occurrences_for_recurring:
        return (
            f"{merchant} appears {occurrences} times, about every "
            f"~{median_interval_days:.0f} days with a similar amount {amount_phrase}, "
            f"but at least {min_occurrences_for_recurring} occurrences are needed to "
            "confirm a stable interval."
        )
    return (
        f"{merchant} appears {occurrences} times, roughly every "
        f"~{median_interval_days:.0f} days with an amount {amount_phrase}, but timing or "
        "amount consistency is not strong enough yet to classify as recurring."
    )


def explain_not_recurring(
    merchant: str,
    occurrences: int,
    amount_variation: float,
    interval_variation: float,
) -> str:
    amount_phrase = f"amount varies by {amount_variation * 100:.0f}%"
    if math.isnan(interval_variation):
        variability_phrase = amount_phrase
    else:
        variability_phrase = f"{amount_phrase}, interval varies by {interval_variation * 100:.0f}%"
    return (
        f"{merchant} appears {occurrences} times, but timing and amounts are too variable "
        f"to classify as recurring ({variability_phrase})."
    )


def explain_insufficient_history(merchant: str, occurrences: int, min_occurrences: int) -> str:
    plural = "transaction" if occurrences == 1 else "transactions"
    return (
        f"{merchant} has only {occurrences} {plural} — at least {min_occurrences} are "
        "needed to assess recurrence."
    )
