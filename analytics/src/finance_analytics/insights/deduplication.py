"""Deterministic insight deduplication (PR-012 §7).

Two insights communicate the same fact if they share type, category,
merchant and comparison period — the same dimensions `rules.py` and
`conversions.py` already use to key at most one insight per
(type, subject, period) combination internally (e.g. one `category_change`
insight per category per month). This module exists as an explicit, tested
safety net for that invariant rather than an implicit assumption, and as
the place a future insight source with looser keying would still be
protected.
"""

from __future__ import annotations

from finance_analytics.insights.models import Insight


def _dedup_key(insight: Insight) -> tuple[str, str | None, str | None, str | None]:
    return (insight.type, insight.category, insight.merchant, insight.comparison_period)


def deduplicate_insights(insights: list[Insight]) -> list[Insight]:
    """Keep one insight per `(type, category, merchant, comparison_period)`.

    When two insights share a key, the higher-confidence one is kept. The
    result preserves the first-seen relative order of surviving keys, so
    deduplicating an already-ranked list keeps it ranked.
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
