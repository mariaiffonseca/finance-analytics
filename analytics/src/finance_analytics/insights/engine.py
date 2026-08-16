"""The Insights Engine entry point (PR-012 §11).

```
Transactions
     |
Deduplication                          (this module)
     |
     +--- Enrichment (PR-011)  ---> category_change_insights   (rules.py)
     |
     +--- Anomaly detection (PR-009)  ---> unusual_transaction_insights (conversions.py)
     |
     +--- Recurring detection (PR-010) ---> recurring_payment_insights (conversions.py)
     |
     +--- spending_trend / income_expense_change / savings_rate_change (rules.py)
     |
     v
Deduplication (deduplication.py) -> Ranking (ranking.py) -> Structured Insights
```

`generate_insights` is the single entry point future callers (a future
API, this project's own notebooks) should use. It does not reimplement
anomaly detection, recurring-transaction classification or categorisation
— it calls PR-009/PR-010/PR-011's own functions and adapts their results.
"""

from __future__ import annotations

import pandas as pd

from finance_analytics.anomalies.detector import detect_anomalies
from finance_analytics.enrichment.models import enrich_transactions
from finance_analytics.insights import conversions, deduplication, ranking, rules
from finance_analytics.insights.models import Insight
from finance_analytics.recurring.detector import detect_recurring_transactions

#: Column added to the working frame to hold PR-011's enriched category —
#: distinct from `category_column` so `rules.category_change_insights` can
#: consume the enriched value while `detect_anomalies`/
#: `detect_recurring_transactions` still see the original raw column
#: exactly as PR-009/PR-010 expect it (their own validated, tested
#: behaviour is not altered by this PR).
_ENRICHED_CATEGORY_COLUMN = "_insights_enriched_category"


def generate_insights(
    frame: pd.DataFrame,
    id_column: str = "id",
    date_column: str = "date",
    amount_column: str = "amount",
    category_column: str = "category",
    merchant_column: str = "merchant",
    currency_column: str = "currency",
    description_column: str = "description",
    top_n: int | None = None,
) -> list[Insight]:
    """Run the full Insights Engine pipeline and return a ranked,
    deduplicated list of `Insight` objects.

    `frame` should be a loaded transaction DataFrame (e.g. from
    `finance_analytics.io.csv.load_transactions_csv`); it does not need to
    be pre-cleaned or pre-deduplicated — this function deduplicates by
    `id_column` itself (matching `enrichment.models.enrich_transactions`'s
    own convention) before calling PR-009/PR-010/PR-011's functions, each
    of which handles rows with missing/invalid dates or amounts on its own.

    `top_n`, if given, returns only the `top_n` highest-ranked insights
    (PR-012 §16: the shape a future API/UI layer would want, e.g. "give me
    the 5 most important insights") — `None` returns all of them.
    Deterministic: the same input always produces the same output list, in
    the same order.
    """
    deduplicated = frame.drop_duplicates(subset=[id_column]).copy()
    deduplicated[id_column] = deduplicated[id_column].astype(str)

    # PR-011: enrichment supplies the category `rules.py`'s period-comparison
    # rules consume, instead of this PR re-deriving categorisation.
    enriched = enrich_transactions(
        deduplicated,
        id_column=id_column,
        merchant_column=merchant_column,
        category_column=category_column,
        description_column=description_column,
    )
    enriched_category_by_id = {e.transaction_id: e.category for e in enriched}
    working = deduplicated.copy()
    working[_ENRICHED_CATEGORY_COLUMN] = working[id_column].map(enriched_category_by_id)

    # PR-009 / PR-010: run against the original raw category/merchant
    # columns, unchanged from how those PRs validated them.
    anomaly_results = detect_anomalies(
        deduplicated,
        id_column=id_column,
        date_column=date_column,
        amount_column=amount_column,
        category_column=category_column,
        merchant_column=merchant_column,
        currency_column=currency_column,
    )
    recurring_results = detect_recurring_transactions(
        deduplicated,
        id_column=id_column,
        date_column=date_column,
        amount_column=amount_column,
        merchant_column=merchant_column,
        currency_column=currency_column,
    )

    insights: list[Insight] = []
    insights += rules.spending_trend_insights(
        working, date_column=date_column, amount_column=amount_column, id_column=id_column
    )
    insights += rules.category_change_insights(
        working,
        date_column=date_column,
        amount_column=amount_column,
        category_column=_ENRICHED_CATEGORY_COLUMN,
        id_column=id_column,
    )
    insights += rules.income_expense_change_insights(
        working, date_column=date_column, amount_column=amount_column, id_column=id_column
    )
    insights += rules.savings_rate_change_insights(
        working, date_column=date_column, amount_column=amount_column, id_column=id_column
    )
    insights += conversions.unusual_transaction_insights(
        anomaly_results,
        deduplicated,
        id_column=id_column,
        category_column=category_column,
        merchant_column=merchant_column,
        amount_column=amount_column,
    )
    insights += conversions.recurring_payment_insights(recurring_results)

    deduped = deduplication.deduplicate_insights(insights)
    ranked = ranking.rank_insights(deduped)
    return ranked[:top_n] if top_n is not None else ranked
