"""Deterministic insight ranking (PR-012 §6).

Ranks by evidence strength (severity), then confidence, then recency, using
sequential stable sorts from least to most significant key — a common,
easy-to-verify way to express a compound sort without needing to negate a
string for "descending". No ML, no learned weights: a small, explainable
ordering, consistent with every other scoring decision in this codebase
(`recurring.scoring.combine_confidence`, `anomalies.detector`'s tier
ordering). This is not a recommendation model (PR-012 §6: "Do not create a
complex recommendation model") — it only orders insights that already
exist.
"""

from __future__ import annotations

from finance_analytics.insights.models import IMPORTANT, INFO, NOTICE, Insight

_SEVERITY_RANK: dict[str, int] = {IMPORTANT: 2, NOTICE: 1, INFO: 0}


def _recency_key(insight: Insight) -> str:
    """The most recent period/date this insight's evidence references, as a
    sortable ISO string. `comparison_period` covers the four period-based
    rules; `metadata["last_seen"]` covers recurring-payment insights.
    Unusual-transaction insights have neither — evidence is one
    transaction, whose age is not part of what makes it noteworthy — so
    they sort as neither older nor newer here and rely on severity/
    confidence to place them.
    """
    if insight.comparison_period:
        return insight.comparison_period
    last_seen = insight.metadata.get("last_seen")
    return last_seen or ""


def rank_insights(insights: list[Insight]) -> list[Insight]:
    """Sort insights by (severity desc, confidence desc, recency desc, id
    asc). Deterministic: the same input list always produces the same
    output order, regardless of the order insights were generated in.
    """
    ordered = sorted(insights, key=lambda insight: insight.id)
    ordered = sorted(ordered, key=_recency_key, reverse=True)
    ordered = sorted(ordered, key=lambda insight: insight.confidence, reverse=True)
    ordered = sorted(
        ordered, key=lambda insight: _SEVERITY_RANK.get(insight.severity, 0), reverse=True
    )
    return ordered
