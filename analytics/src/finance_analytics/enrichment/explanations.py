"""Deterministic, template-based explanations for categorisation results.

No LLM is used to generate these (PR-011, matching the convention already
established by `anomalies.explanations` and `recurring.explanations`).
Every sentence is a fixed template filled in with the same values used for
categorisation in `categories.py`, so the same input always produces the
same sentence and every rule is explainable (PR-011 Acceptance Criteria).
"""

from __future__ import annotations


def explain_known_merchant_rule(normalised_merchant: str, category: str) -> str:
    return f"'{normalised_merchant}' is a known merchant, categorised as {category}."


def explain_known_description_rule(category: str) -> str:
    return f"Transaction description matched a known pattern for {category}."


def explain_existing_category(category: str) -> str:
    return (
        f"No merchant or description rule matched; the imported category "
        f"'{category}' is already part of the known taxonomy and is trusted."
    )


def explain_fallback(normalised_merchant: str | None, category: str) -> str:
    subject = f"'{normalised_merchant}'" if normalised_merchant else "This transaction"
    return (
        f"{subject} did not match a known merchant or description rule, and its "
        f"imported category was missing or not recognised — marked '{category}'."
    )


#: Method name (from `categories.CONFIDENCE_BY_METHOD`) -> the explanation
#: function that documents it. `explain()` uses this instead of an if/elif
#: chain so adding a new method tier can't forget to wire up its explanation.
_EXPLAINERS = {
    "known_merchant_rule": lambda merchant, category: explain_known_merchant_rule(
        merchant, category
    ),
    "known_description_rule": lambda merchant, category: explain_known_description_rule(category),
    "existing_category": lambda merchant, category: explain_existing_category(category),
    "fallback": lambda merchant, category: explain_fallback(merchant, category),
}


def explain(method: str, normalised_merchant: str | None, category: str) -> str:
    """Build the deterministic explanation for a `categorise()` result."""
    return _EXPLAINERS[method](normalised_merchant, category)
