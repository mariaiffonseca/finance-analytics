"""The enriched-transaction result model and pipeline entry point (PR-011).

```
Raw transaction
      |
Merchant normalisation   (merchants.normalise_merchant)
      |
Category assignment      (categories.categorise)
      |
Enriched transaction
```

`enrich_transactions` is the reusable entry point future analytics should
build on (PR-011, "Keep the enrichment layer reusable by future analytics").
It does not persist, display, or otherwise act on its output — it only
produces a structured, explainable result per transaction.
"""

from __future__ import annotations

from collections.abc import Mapping
from dataclasses import dataclass

import pandas as pd

from finance_analytics.enrichment.categories import categorise
from finance_analytics.enrichment.explanations import explain
from finance_analytics.enrichment.merchants import normalise_merchant


@dataclass(frozen=True)
class EnrichedTransaction:
    """The result of enriching a single transaction.

    All `raw_*` fields preserve the original imported value exactly — `None`
    only for a genuinely missing value (`NaN`/empty), never a cleaned or
    reformatted version of it (PR-011: "Preserve all raw transaction
    information"). `normalised_merchant` is the one derived merchant field;
    `category`/`category_confidence`/`categorisation_method`/`reason`
    together make every categorisation decision traceable back to the rule
    that produced it.
    """

    transaction_id: str
    raw_merchant: str | None
    normalised_merchant: str | None
    raw_category: str | None
    raw_description: str | None
    category: str
    category_confidence: float
    categorisation_method: str
    reason: str


def _raw_or_none(value: object) -> str | None:
    return None if pd.isna(value) else str(value)


def enrich_transaction(
    transaction_id: object,
    raw_merchant: object,
    raw_category: object,
    raw_description: object,
    *,
    merchant_aliases: Mapping[str, str] | None = None,
    merchant_rules: Mapping[str, str] | None = None,
    description_rules: Mapping[str, str] | None = None,
) -> EnrichedTransaction:
    """Enrich a single transaction's merchant/category fields.

    Deterministic: the same four inputs always produce the same result.
    `merchant_aliases`/`merchant_rules`/`description_rules` are forwarded to
    `merchants.normalise_merchant`/`categories.categorise` and exist so
    tests can exercise the pipeline with synthetic configuration.
    """
    normalised = normalise_merchant(raw_merchant, aliases=merchant_aliases)
    result = categorise(
        normalised,
        raw_category,
        raw_description,
        merchant_rules=merchant_rules,
        description_rules=description_rules,
    )
    reason = explain(result.method, normalised, result.category)

    return EnrichedTransaction(
        transaction_id=str(transaction_id),
        raw_merchant=_raw_or_none(raw_merchant),
        normalised_merchant=normalised,
        raw_category=_raw_or_none(raw_category),
        raw_description=_raw_or_none(raw_description),
        category=result.category,
        category_confidence=result.confidence,
        categorisation_method=result.method,
        reason=reason,
    )


def enrich_transactions(
    frame: pd.DataFrame,
    id_column: str = "id",
    merchant_column: str = "merchant",
    category_column: str = "category",
    description_column: str = "description",
) -> list[EnrichedTransaction]:
    """Enrich every transaction in `frame`.

    Unlike `anomalies.detect_anomalies`/`recurring.detect_recurring_transactions`,
    this does not require valid `date`/`amount`, nor does it drop expense-only
    or history-based rows — merchant normalisation and categorisation need
    neither (PR-011: "Do not silently discard problematic rows"). A row with
    a missing merchant is still enriched: `normalised_merchant` is `None`,
    but `category` may still resolve via the description or existing-category
    tier. The only row-level filtering is deduplication by `id_column`
    (`drop_duplicates`, keeping the first occurrence) — a repeated `id` is
    the same transaction represented twice, not two transactions to enrich
    separately, matching the deduplication convention `anomalies`/`recurring`
    expect their caller to already have applied.

    Missing columns are tolerated (treated as entirely missing values) so
    this can run against a minimal frame in tests without every column
    present.
    """
    deduplicated = frame.drop_duplicates(subset=[id_column])

    return [
        enrich_transaction(
            row[id_column],
            row.get(merchant_column),
            row.get(category_column),
            row.get(description_column),
        )
        for _, row in deduplicated.iterrows()
    ]
