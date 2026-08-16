"""Deterministic transaction categorisation (PR-011).

**Taxonomy.** `KNOWN_CATEGORIES` is not invented: it is the exact set of
category values already present in the project's transaction data
(`docs/project/02_DOMAIN_MODEL.md`'s `Category` entity, which the domain
model says "should remain stable to preserve historical analytics"). PR-011's
Critical Rule says to preserve reliable existing values rather than replace
them with a new scheme, and the EDA/data-quality assessment
(`notebooks/05_merchant_and_category_analysis.ipynb`) found the raw
`category` column already consistent — no casing variants, no typos, no
conflicting values for the same transaction.

**Priority order**, evaluated top to bottom, first match wins:

1. **Known merchant rule** (`MERCHANT_CATEGORY_RULES`) — the normalised
   merchant is a merchant this project has unambiguous historical evidence
   for (every observed occurrence agreed on one category). Highest
   confidence: it reflects the merchant's own consistent history, so it is
   trusted even over a conflicting raw `category` value on one particular
   row (a merchant rule wins a conflict — see `test_enrichment_categories.py`
   for a worked conflicting-signal example).
2. **Known description rule** (`DESCRIPTION_KEYWORD_RULES`) — only reached
   when no merchant rule matched (unknown or missing merchant). A deterministic
   keyword substring in the transaction description.
3. **Existing trusted category** — the imported `category` value is present
   and is a member of `KNOWN_CATEGORIES`. Reached when neither rule above
   applies; this is what keeps an already-reliable raw category from being
   needlessly overridden or discarded.
4. **Fallback** (`FALLBACK_CATEGORY`) — none of the above. The transaction
   is never forced into a guessed category; `"Uncategorised"` is an
   explicit, honest outcome, not a wrong answer disguised as a real one.

**Why a merchant-rule table at all, given the raw category is already
reliable?** Its value is not re-deriving what a row already states — it is
making that knowledge reusable for a *different* row from the same merchant
that has a missing or blank category (see `transaction_id=21` in the
fixture: merchant missing, but its own raw category is still present and
trusted at tier 3). Every entry in `MERCHANT_CATEGORY_RULES` is seeded from
the fixture's own unambiguous merchant -> category history, documented in
the notebook with the supporting occurrence count for each — nothing is
guessed. `"Test Merchant"` (used only by two deliberately-invalid QA fixture
rows — see PR-008's EDA, section on cleaning the fixture) is intentionally
excluded: it is test infrastructure, not evidence about a real merchant.

**Why not use amount as a signal.** PR-011 explicitly warns against
categorising by amount alone without strong justification, and no category
here needs it: every rule above is resolvable from merchant/description/
existing-category alone.

**Confidence is not a probability.** `CONFIDENCE_BY_METHOD` assigns a fixed,
documented score per method tier — an ordinal ranking of how much evidence
backs the assignment, not a calibrated probability (PR-011, Unknown /
Uncertain Transactions: "Do not call confidence a probability unless
calibrated"). Nothing here has been fit or validated against labelled
outcomes.
"""

from __future__ import annotations

from collections.abc import Mapping
from dataclasses import dataclass

import pandas as pd

#: The project's existing category taxonomy — see module docstring.
KNOWN_CATEGORIES: frozenset[str] = frozenset(
    {
        "Food & Dining",
        "Groceries",
        "Subscriptions",
        "Utilities",
        "Health",
        "Transport",
        "Travel",
        "Shopping",
        "Income",
        "Cash",
    }
)

#: Explicit fallback for a transaction none of the rules below can place —
#: never guessed, always this exact label.
FALLBACK_CATEGORY = "Uncategorised"

#: Casefolded normalised-merchant -> category. Seeded entirely from the
#: fixture's own unambiguous merchant history — see module docstring and
#: notebook 05 for the supporting occurrence count behind each entry.
#: `"Test Merchant"` is deliberately excluded (QA fixture, not a real
#: merchant).
MERCHANT_CATEGORY_RULES: Mapping[str, str] = {
    "coffee corner": "Food & Dining",
    "continente": "Groceries",
    "spotify": "Subscriptions",
    "o pescador": "Food & Dining",
    "edp": "Utilities",
    "farmácia central": "Health",
    "cp": "Transport",
    "tap air": "Travel",
    "mediamarkt": "Shopping",
    "café central": "Food & Dining",
    "netflix": "Subscriptions",
    "employer payroll": "Income",
    "atm": "Cash",
    "galp": "Transport",
    "freelance client": "Income",
}

#: Casefolded description substring -> category, used only when no merchant
#: rule matched. `"subscription"` is the one entry backed by cross-merchant
#: evidence: both `Spotify` and `Netflix` rows carry the description
#: "Monthly subscription" and both are `Subscriptions` — a pattern that
#: holds independently of which merchant it is, unlike a single-occurrence
#: phrase (e.g. "Monthly salary", seen once) which is not codified here as
#: a rule; see notebook 05's Limitations section.
DESCRIPTION_KEYWORD_RULES: Mapping[str, str] = {
    "subscription": "Subscriptions",
}

#: Fixed confidence per method tier — see module docstring ("Confidence is
#: not a probability").
CONFIDENCE_BY_METHOD: Mapping[str, float] = {
    "known_merchant_rule": 1.0,
    "known_description_rule": 0.75,
    "existing_category": 0.5,
    "fallback": 0.0,
}


@dataclass(frozen=True)
class CategorisationResult:
    """The outcome of categorising a single transaction."""

    category: str
    confidence: float
    method: str


def _clean_text(value: object) -> str | None:
    if pd.isna(value):
        return None
    text = str(value).strip()
    return text or None


def categorise(
    normalised_merchant: str | None,
    raw_category: object,
    description: object,
    *,
    merchant_rules: Mapping[str, str] | None = None,
    description_rules: Mapping[str, str] | None = None,
) -> CategorisationResult:
    """Categorise a transaction using the priority order documented above.

    Deterministic: the same three inputs always produce the same result.
    `merchant_rules`/`description_rules` default to the module-level rule
    tables and exist as parameters so tests can exercise the priority chain
    with synthetic rules without mutating shared module state.
    """
    rules = MERCHANT_CATEGORY_RULES if merchant_rules is None else merchant_rules
    keywords = DESCRIPTION_KEYWORD_RULES if description_rules is None else description_rules

    if normalised_merchant is not None:
        category = rules.get(normalised_merchant.casefold())
        if category is not None:
            return CategorisationResult(
                category, CONFIDENCE_BY_METHOD["known_merchant_rule"], "known_merchant_rule"
            )

    description_text = _clean_text(description)
    if description_text is not None:
        folded = description_text.casefold()
        for keyword, category in keywords.items():
            if keyword in folded:
                return CategorisationResult(
                    category,
                    CONFIDENCE_BY_METHOD["known_description_rule"],
                    "known_description_rule",
                )

    category_text = _clean_text(raw_category)
    if category_text is not None and category_text in KNOWN_CATEGORIES:
        return CategorisationResult(
            category_text, CONFIDENCE_BY_METHOD["existing_category"], "existing_category"
        )

    return CategorisationResult(FALLBACK_CATEGORY, CONFIDENCE_BY_METHOD["fallback"], "fallback")
