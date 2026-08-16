"""The structured Insight result model (PR-012 §1).

Every insight generator in this package (`rules.py`, `conversions.py`)
returns `Insight` objects built from this one model, so `engine.py` can
rank, deduplicate and return a single uniform list regardless of which rule
produced each insight. `Insight.to_dict()` renders a plain, JSON-serialisable
dict — the shape a future API layer would return as-is (PR-012 §16); this
PR does not implement that API, only a model shaped for it.
"""

from __future__ import annotations

from collections.abc import Mapping
from dataclasses import dataclass, field
from typing import Any

#: Controlled severity vocabulary (PR-012 §5). Deliberately small and
#: deliberately neutral — severity communicates how much attention an
#: insight deserves, not financial danger. `IMPORTANT` is used for large,
#: clearly-evidenced deviations (a confirmed recurring charge, a strongly
#: anomalous transaction, a large period-over-period swing), never to imply
#: the underlying spending is bad.
INFO = "INFO"
NOTICE = "NOTICE"
IMPORTANT = "IMPORTANT"

SEVERITIES: tuple[str, ...] = (INFO, NOTICE, IMPORTANT)

#: Controlled insight-type vocabulary (PR-012 §2).
SPENDING_TREND = "spending_trend"
CATEGORY_CHANGE = "category_change"
UNUSUAL_TRANSACTION = "unusual_transaction"
RECURRING_PAYMENT = "recurring_payment"
INCOME_EXPENSE_CHANGE = "income_expense_change"
SAVINGS_RATE_CHANGE = "savings_rate_change"

INSIGHT_TYPES: tuple[str, ...] = (
    SPENDING_TREND,
    CATEGORY_CHANGE,
    UNUSUAL_TRANSACTION,
    RECURRING_PAYMENT,
    INCOME_EXPENSE_CHANGE,
    SAVINGS_RATE_CHANGE,
)


@dataclass(frozen=True)
class Insight:
    """A single structured, evidence-backed insight.

    `id` is deterministic and human-readable (e.g.
    `"category_change:Groceries:2026-03"`) rather than a random/opaque
    token, so the same input always produces the same id — useful for
    deduplication (`deduplication.py`), tests, and a future API consumer
    that wants a stable identifier per fact.

    `confidence` is a heuristic 0-1 score reflecting evidence quality
    (history length, pattern consistency, signal strength — PR-012 §4), not
    a calibrated probability, matching the convention already established
    by `recurring.scoring.combine_confidence` and
    `enrichment.categories.CONFIDENCE_BY_METHOD`.

    `metadata` carries whatever a specific insight type needs to explain
    itself (PR-012 §9: `current_value`, `previous_value`, `change_percent`,
    `comparison_period`, `threshold`, `source_feature`, ...) — deliberately
    an open `dict` rather than a fixed set of fields, since different
    insight types need different explanatory context.
    """

    id: str
    type: str
    title: str
    description: str
    severity: str
    confidence: float
    related_transaction_ids: tuple[str, ...] = ()
    metadata: Mapping[str, Any] = field(default_factory=dict)
    category: str | None = None
    merchant: str | None = None
    amount: float | None = None
    comparison_period: str | None = None

    def to_dict(self) -> dict[str, Any]:
        """Plain, JSON-serialisable representation (PR-012 §16 — Future API
        Boundary). No FastAPI/HTTP layer is implemented in this PR; this is
        only the shape a serialiser would consume."""
        return {
            "id": self.id,
            "type": self.type,
            "title": self.title,
            "description": self.description,
            "severity": self.severity,
            "confidence": self.confidence,
            "related_transaction_ids": list(self.related_transaction_ids),
            "metadata": dict(self.metadata),
            "category": self.category,
            "merchant": self.merchant,
            "amount": self.amount,
            "comparison_period": self.comparison_period,
        }
