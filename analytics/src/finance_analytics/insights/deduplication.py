"""Deterministic insight deduplication (PR-012 §7).

Two period-comparison insights (`rules.py`: `spending_trend`,
`category_change`, `income_expense_change`, `savings_rate_change`)
communicate the same fact if they share type, category, merchant and
comparison period — the same dimensions `rules.py` already uses to key at
most one insight per (type, subject, period) combination internally (e.g.
one `category_change` insight per category per month). This module exists
as an explicit, tested safety net for that invariant rather than an implicit
assumption, and as the place a future insight source with looser keying
would still be protected.

Conversion-based insights (`conversions.py`: `unusual_transaction`,
`recurring_payment`) do *not* share that key: two distinct anomalous
transactions for the same merchant/category both have `comparison_period=
None`, and a merchant billed in multiple currencies is deliberately more
than one `RecurringResult` (see that module's own docstring). Keying those
types the same way would silently discard separate, real facts, so they are
deduplicated on their own already-unique `id` instead (per transaction id,
or per merchant+currency).
"""

from __future__ import annotations

from finance_analytics.insights.models import (
    CATEGORY_CHANGE,
    INCOME_EXPENSE_CHANGE,
    SAVINGS_RATE_CHANGE,
    SPENDING_TREND,
    Insight,
)

#: Insight types for which `(type, category, merchant, comparison_period)`
#: uniquely identifies a fact. Conversion-based types are deliberately
#: excluded — see module docstring.
_PERIOD_COMPARISON_TYPES = frozenset(
    {SPENDING_TREND, CATEGORY_CHANGE, INCOME_EXPENSE_CHANGE, SAVINGS_RATE_CHANGE}
)


def _dedup_key(insight: Insight) -> tuple:
    if insight.type in _PERIOD_COMPARISON_TYPES:
        return (insight.type, insight.category, insight.merchant, insight.comparison_period)
    return (insight.type, insight.id)


def deduplicate_insights(insights: list[Insight]) -> list[Insight]:
    """Keep one insight per fact, per `_dedup_key`.

    Period-comparison insights are keyed by
    `(type, category, merchant, comparison_period)`; conversion-based
    insights are keyed by their own already-unique `id` — see module
    docstring. When two insights share a key, the higher-confidence one is
    kept. The result preserves the first-seen relative order of surviving
    keys, so deduplicating an already-ranked list keeps it ranked.
    """
    best: dict[tuple, Insight] = {}
    order: list[tuple] = []
    for insight in insights:
        key = _dedup_key(insight)
        if key not in best:
            order.append(key)
            best[key] = insight
        elif insight.confidence > best[key].confidence:
            best[key] = insight

    return [best[key] for key in order]
