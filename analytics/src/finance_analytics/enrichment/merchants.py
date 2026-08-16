"""Deterministic merchant-name normalisation (PR-011).

**No fuzzy matching.** The only two operations are (1) structural cleanup —
trimming and collapsing whitespace — and (2) case-insensitive lookup against
an explicit, curated alias table. Two strings are only ever folded into one
merchant identity if they are identical once cleaned and casefolded, or if a
human has explicitly recorded them as aliases. Nothing here guesses that two
*different* strings (e.g. "Amazon" and "Amzn") refer to the same merchant —
PR-011's Critical Rule requires evidence for that, and none exists in the
current dataset.

**`MERCHANT_ALIASES` is empty by design.** The project's test fixture
(`data/raw/finance_analytics_test_transactions.csv`) has no casing,
whitespace or punctuation variants of the same merchant — every merchant
name already appears consistently formatted across all its occurrences (see
`notebooks/05_merchant_and_category_analysis.ipynb`, Data Quality section).
There is therefore no evidence to seed this table with yet. The mechanism
itself (case-insensitive alias folding, e.g. "NETFLIX" / "netflix.com" both
resolving to "Netflix") is still implemented and covered by tests using
synthetic examples, since real bank CSV exports commonly need it. Add an
entry here only when a future dataset provides evidence of an actual alias
— never speculatively.
"""

from __future__ import annotations

import re
from collections.abc import Mapping

import pandas as pd

#: Casefolded raw-merchant string -> canonical display name. See module
#: docstring for why this is currently empty.
MERCHANT_ALIASES: Mapping[str, str] = {}

_WHITESPACE_RE = re.compile(r"\s+")


def normalise_merchant(raw: object, *, aliases: Mapping[str, str] | None = None) -> str | None:
    """Deterministically normalise a raw merchant string.

    Returns `None` for missing input — `NaN`, `None`, empty string, or
    whitespace-only — never a placeholder like `"Unknown"`; that is a
    categorisation-layer concern (`categories.py`), not a normalisation
    one. Otherwise the value is trimmed and internal whitespace is
    collapsed to a single space.

    Casing is preserved as given: this dataset's raw merchant names are
    already correctly brand-cased (`TAP Air`, `EDP`, `CP`, `MediaMarkt`);
    blindly title-casing every value would corrupt those acronyms and
    proper names rather than fix anything (PR-011's Critical Rule — preserve
    reliable values instead of "improving" them without evidence).

    If the cleaned value's casefolded form matches a key in `aliases`
    (`MERCHANT_ALIASES` by default), the curated canonical name is returned
    instead. This is the only place two differently-formatted raw strings
    are allowed to fold into a single merchant identity — see the module
    docstring.
    """
    if pd.isna(raw):
        return None

    text = _WHITESPACE_RE.sub(" ", str(raw).strip()).strip()
    if not text:
        return None

    table = MERCHANT_ALIASES if aliases is None else aliases
    canonical = table.get(text.casefold())
    return canonical if canonical is not None else text
