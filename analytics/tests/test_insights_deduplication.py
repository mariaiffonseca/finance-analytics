from finance_analytics.insights.deduplication import deduplicate_insights
from finance_analytics.insights.models import NOTICE, Insight


def _insight(
    id_,
    type_="category_change",
    confidence=0.5,
    category="Groceries",
    merchant=None,
    comparison_period="2026-03",
):
    return Insight(
        id=id_,
        type=type_,
        title="t",
        description="d",
        severity=NOTICE,
        confidence=confidence,
        category=category,
        merchant=merchant,
        comparison_period=comparison_period,
    )


def test_equivalent_insights_are_deduplicated_keeping_the_higher_confidence_one():
    weaker = _insight("a", confidence=0.4)
    stronger = _insight("b", confidence=0.9)

    result = deduplicate_insights([weaker, stronger])

    assert len(result) == 1
    assert result[0].id == "b"


def test_insights_with_different_keys_are_both_kept():
    groceries = _insight("a", category="Groceries")
    health = _insight("b", category="Health")

    result = deduplicate_insights([groceries, health])

    assert {i.id for i in result} == {"a", "b"}


def test_different_types_are_not_deduplicated_against_each_other():
    trend = _insight("a", type_="spending_trend", category=None)
    anomaly = _insight("b", type_="unusual_transaction", category=None, comparison_period=None)

    result = deduplicate_insights([trend, anomaly])

    assert {i.id for i in result} == {"a", "b"}


def test_different_comparison_periods_are_not_deduplicated_against_each_other():
    march = _insight("a", comparison_period="2026-03")
    april = _insight("b", comparison_period="2026-04")

    result = deduplicate_insights([march, april])

    assert {i.id for i in result} == {"a", "b"}


def test_preserves_first_seen_order_of_surviving_keys():
    first = _insight("a", category="Health")
    second = _insight("b", category="Groceries")
    duplicate_of_first = _insight("c", category="Health", confidence=0.99)

    result = deduplicate_insights([first, second, duplicate_of_first])

    # "Health" survives as the stronger duplicate ("c"), but keeps its
    # original (first-seen) position ahead of "Groceries".
    assert [i.id for i in result] == ["c", "b"]


def test_empty_list_returns_empty_list():
    assert deduplicate_insights([]) == []


def test_distinct_unusual_transactions_for_same_merchant_are_both_kept():
    # Two separate anomalous transactions for the same merchant/category
    # both have comparison_period=None — the generic period-comparison key
    # would otherwise collapse them into one and silently drop a real
    # anomaly.
    first = _insight("unusual_transaction:100", type_="unusual_transaction", comparison_period=None)
    second = _insight(
        "unusual_transaction:200", type_="unusual_transaction", comparison_period=None
    )

    result = deduplicate_insights([first, second])

    assert {i.id for i in result} == {"unusual_transaction:100", "unusual_transaction:200"}


def test_distinct_recurring_payments_for_same_merchant_different_currency_are_both_kept():
    # A merchant billed in multiple currencies is deliberately more than
    # one RecurringResult (recurring/detector.py) — currency isn't in the
    # generic key, so this relies on the id (which includes currency)
    # instead.
    eur = _insight(
        "recurring_payment:Netflix:EUR",
        type_="recurring_payment",
        merchant="Netflix",
        category=None,
        comparison_period=None,
    )
    usd = _insight(
        "recurring_payment:Netflix:USD",
        type_="recurring_payment",
        merchant="Netflix",
        category=None,
        comparison_period=None,
    )

    result = deduplicate_insights([eur, usd])

    assert {i.id for i in result} == {
        "recurring_payment:Netflix:EUR",
        "recurring_payment:Netflix:USD",
    }
