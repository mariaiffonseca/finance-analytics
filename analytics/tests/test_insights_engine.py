"""End-to-end tests for the Insights Engine (PR-012 §14 — controlled
scenarios A-E), plus integration checks against the project's own fixture.
"""

from __future__ import annotations

import pandas as pd

from finance_analytics.anomalies.detector import detect_anomalies
from finance_analytics.insights.engine import generate_insights
from finance_analytics.insights.models import (
    CATEGORY_CHANGE,
    RECURRING_PAYMENT,
    SPENDING_TREND,
    UNUSUAL_TRANSACTION,
)
from finance_analytics.io.csv import load_transactions_csv
from finance_analytics.recurring.detector import detect_recurring_transactions

FIXTURE_PATH = "data/raw/finance_analytics_test_transactions.csv"


def _clean_fixture() -> pd.DataFrame:
    """The same three-step cleaning `notebooks/02-05` apply before
    behavioural analysis: drop structurally invalid rows, drop the
    duplicate id, drop the QA-only row disguised as a transaction."""
    raw = load_transactions_csv(FIXTURE_PATH)
    clean = raw.dropna(subset=["date", "amount"]).drop_duplicates(subset=["id"])
    return clean[~clean["description"].fillna("").str.contains("test", case=False)]


def _row(id_, date, amount, category="Food & Dining", merchant="Merchant"):
    return {
        "id": id_,
        "date": pd.Timestamp(date),
        "amount": amount,
        "category": category,
        "merchant": merchant,
        "description": "",
        "currency": "EUR",
    }


# --- Scenario A-E (PR-012 §14) ---------------------------------------------


def test_scenario_a_stable_spending_produces_no_spending_trend_insight():
    rows = [_row(f"j{i}", f"2026-01-{d:02d}", -20.0) for i, d in enumerate([5, 10, 15, 20, 25])] + [
        _row(f"f{i}", f"2026-02-{d:02d}", -20.5) for i, d in enumerate([3, 9, 15, 21, 28])
    ]
    frame = pd.DataFrame(rows)

    insights = generate_insights(frame)

    assert not any(i.type == SPENDING_TREND for i in insights)


def test_scenario_b_significant_spending_increase_produces_a_spending_trend_insight():
    rows = [_row(f"j{i}", f"2026-01-{d:02d}", -20.0) for i, d in enumerate([5, 10, 15, 20, 25])] + [
        _row(f"f{i}", f"2026-02-{d:02d}", -50.0) for i, d in enumerate([3, 9, 15, 21, 28])
    ]
    frame = pd.DataFrame(rows)

    insights = generate_insights(frame)

    trend_insights = [i for i in insights if i.type == SPENDING_TREND]
    assert len(trend_insights) == 1
    assert trend_insights[0].title == "Spending increased"


def test_scenario_c_unusual_transaction_produces_an_anomaly_insight():
    # A long, stable history of ~20 EUR coffees, then one wildly larger
    # purchase for the same merchant/category.
    rows = [
        _row(f"h{i}", f"2026-01-{d:02d}", -20.0, merchant="Coffee Corner")
        for i, d in enumerate([2, 4, 6, 8, 10, 12])
    ] + [_row("spike", "2026-01-14", -900.0, merchant="Coffee Corner")]
    frame = pd.DataFrame(rows)

    insights = generate_insights(frame)

    anomaly_insights = [i for i in insights if i.type == UNUSUAL_TRANSACTION]
    assert len(anomaly_insights) == 1
    assert anomaly_insights[0].related_transaction_ids == ("spike",)


def test_scenario_d_recurring_subscription_produces_a_recurring_insight():
    dates = pd.date_range("2026-01-01", periods=5, freq="30D")
    rows = [_row(str(i), d, -9.99, merchant="Spotify") for i, d in enumerate(dates)]
    frame = pd.DataFrame(rows)

    insights = generate_insights(frame)

    recurring_insights = [i for i in insights if i.type == RECURRING_PAYMENT]
    assert len(recurring_insights) == 1
    assert recurring_insights[0].merchant == "Spotify"


def test_scenario_e_insufficient_history_produces_no_period_insights():
    # A single month of data: nothing to compare month-over-month against.
    rows = [_row(f"a{i}", f"2026-01-{d:02d}", -20.0) for i, d in enumerate([5, 10, 15])]
    frame = pd.DataFrame(rows)

    insights = generate_insights(frame)

    period_types = {SPENDING_TREND, CATEGORY_CHANGE}
    assert not any(i.type in period_types for i in insights)


# --- Integration with the project fixture -----------------------------------


def test_real_fixture_produces_the_expected_insight_types():
    insights = generate_insights(_clean_fixture())

    types = {i.type for i in insights}
    # Category-change, income/expense-change and savings-rate-change are all
    # suppressed on this fixture for documented, evidence-based reasons (see
    # notebooks/06_insights_engine.ipynb) — only these three types survive.
    assert types == {SPENDING_TREND, UNUSUAL_TRANSACTION, RECURRING_PAYMENT}


def test_engine_does_not_reimplement_anomaly_or_recurring_detection():
    clean = _clean_fixture()

    insights = generate_insights(clean)
    direct_anomalies = [r for r in detect_anomalies(clean) if r.is_anomaly]
    direct_recurring = [
        r
        for r in detect_recurring_transactions(clean)
        if r.classification in {"Recurring", "Possible recurring"}
    ]

    engine_anomaly_ids = {
        i.related_transaction_ids[0] for i in insights if i.type == UNUSUAL_TRANSACTION
    }
    engine_recurring_merchants = {i.merchant for i in insights if i.type == RECURRING_PAYMENT}

    assert engine_anomaly_ids == {str(r.transaction_id) for r in direct_anomalies}
    assert engine_recurring_merchants == {r.merchant for r in direct_recurring}


def test_generate_insights_is_deterministic():
    clean = _clean_fixture()

    first_run = generate_insights(clean)
    second_run = generate_insights(clean)

    assert first_run == second_run


def test_top_n_limits_and_preserves_rank_order():
    clean = _clean_fixture()

    full = generate_insights(clean)
    limited = generate_insights(clean, top_n=3)

    assert limited == full[:3]


def test_every_insight_is_json_serialisable():
    import json

    for insight in generate_insights(_clean_fixture()):
        json.dumps(insight.to_dict())


def test_duplicate_transaction_ids_are_not_double_counted():
    with_duplicate = pd.concat(
        [
            _clean_fixture(),
            _clean_fixture().iloc[[0]],  # re-add one row as a duplicate id
        ],
        ignore_index=True,
    )

    assert generate_insights(with_duplicate) == generate_insights(_clean_fixture())
