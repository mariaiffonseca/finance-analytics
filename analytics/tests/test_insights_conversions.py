import pandas as pd

from finance_analytics.anomalies.detector import AnomalyResult
from finance_analytics.insights.conversions import (
    recurring_payment_insights,
    unusual_transaction_insights,
)
from finance_analytics.insights.models import IMPORTANT, INFO, NOTICE, UNUSUAL_TRANSACTION
from finance_analytics.recurring.detector import RecurringResult


def _frame():
    return pd.DataFrame(
        [
            {
                "id": "1",
                "category": "Shopping",
                "merchant": "MediaMarkt",
                "amount": -899.0,
            },
            {
                "id": "2",
                "category": "Food & Dining",
                "merchant": "Coffee Corner",
                "amount": -12.5,
            },
        ]
    )


# --- Unusual transaction ---------------------------------------------------


def test_flagged_anomaly_becomes_an_insight_without_recomputing_the_score():
    # A hand-built AnomalyResult, not `detect_anomalies` — proves this
    # module only repackages an existing result (PR-012 acceptance
    # criterion: "Does not reimplement anomaly detection").
    result = AnomalyResult(
        transaction_id="1",
        anomaly_score=17.99,
        is_anomaly=True,
        method="global_relative_robust_z",
        reason="Amount is 18.0x higher than your typical transaction.",
        reference_context={"global_median": 49.95},
    )

    insights = unusual_transaction_insights([result], _frame())

    assert len(insights) == 1
    insight = insights[0]
    assert insight.type == UNUSUAL_TRANSACTION
    assert insight.description == result.reason  # verbatim, not regenerated
    assert insight.metadata["anomaly_score"] == 17.99
    assert insight.metadata["method"] == "global_relative_robust_z"
    assert insight.merchant == "MediaMarkt"
    assert insight.category == "Shopping"
    assert insight.amount == -899.0
    assert insight.related_transaction_ids == ("1",)


def test_unflagged_anomaly_produces_no_insight():
    result = AnomalyResult(
        transaction_id="2",
        anomaly_score=0.4,
        is_anomaly=False,
        method="global_relative_robust_z",
        reason="Amount is close to your typical transaction.",
        reference_context={},
    )

    assert unusual_transaction_insights([result], _frame()) == []


def test_insufficient_history_anomaly_produces_no_insight():
    result = AnomalyResult(
        transaction_id="2",
        anomaly_score=None,
        is_anomaly=False,
        method="insufficient_history",
        reason="Not enough transaction history to evaluate this transaction.",
        reference_context={},
    )

    assert unusual_transaction_insights([result], _frame()) == []


def test_high_zscore_escalates_severity_to_important():
    borderline = AnomalyResult(
        transaction_id="1",
        anomaly_score=3.1,
        is_anomaly=True,
        method="global_relative_robust_z",
        reason="borderline",
        reference_context={},
    )
    extreme = AnomalyResult(
        transaction_id="1",
        anomaly_score=17.99,
        is_anomaly=True,
        method="global_relative_robust_z",
        reason="extreme",
        reference_context={},
    )

    assert unusual_transaction_insights([borderline], _frame())[0].severity == NOTICE
    assert unusual_transaction_insights([extreme], _frame())[0].severity == IMPORTANT


def test_merchant_tier_is_more_confident_than_global_tier():
    merchant_tier = AnomalyResult(
        transaction_id="1",
        anomaly_score=4.0,
        is_anomaly=True,
        method="merchant_relative_robust_z",
        reason="r",
        reference_context={},
    )
    global_tier = AnomalyResult(
        transaction_id="1",
        anomaly_score=4.0,
        is_anomaly=True,
        method="global_relative_robust_z",
        reason="r",
        reference_context={},
    )

    merchant_confidence = unusual_transaction_insights([merchant_tier], _frame())[0].confidence
    global_confidence = unusual_transaction_insights([global_tier], _frame())[0].confidence

    assert merchant_confidence > global_confidence


def test_transaction_missing_from_frame_is_skipped_safely():
    result = AnomalyResult(
        transaction_id="does-not-exist",
        anomaly_score=10.0,
        is_anomaly=True,
        method="global_relative_robust_z",
        reason="r",
        reference_context={},
    )

    assert unusual_transaction_insights([result], _frame()) == []


# --- Recurring payment -------------------------------------------------


def _recurring_result(classification, confidence_score=0.8, merchant="Spotify"):
    return RecurringResult(
        merchant=merchant,
        currency="EUR",
        is_recurring=classification == "Recurring",
        classification=classification,
        confidence_score=confidence_score,
        frequency="Monthly",
        occurrences=4,
        median_amount=9.99,
        amount_variation=0.0,
        median_interval_days=30.0,
        interval_variation=0.02,
        first_seen=pd.Timestamp("2026-01-01"),
        last_seen=pd.Timestamp("2026-04-01"),
        reason="Spotify appears 4 times with a stable monthly interval.",
        transaction_ids=["a", "b", "c", "d"],
    )


def test_recurring_result_becomes_an_insight_without_recomputing_confidence():
    result = _recurring_result("Recurring", confidence_score=0.83)

    insights = recurring_payment_insights([result])

    assert len(insights) == 1
    insight = insights[0]
    assert insight.confidence == 0.83  # taken directly from the source result
    assert insight.description == result.reason
    assert insight.severity == NOTICE
    assert insight.related_transaction_ids == ("a", "b", "c", "d")
    assert insight.merchant == "Spotify"


def test_possible_recurring_gets_a_lower_severity_than_recurring():
    recurring = recurring_payment_insights([_recurring_result("Recurring")])[0]
    possible = recurring_payment_insights([_recurring_result("Possible recurring")])[0]

    assert recurring.severity == NOTICE
    assert possible.severity == INFO


def test_not_recurring_and_insufficient_history_produce_no_insight():
    results = [
        _recurring_result("Not recurring"),
        RecurringResult(
            merchant="Netflix",
            currency="EUR",
            is_recurring=False,
            classification="Insufficient history",
            confidence_score=None,
            frequency="Unknown",
            occurrences=1,
            median_amount=15.99,
            amount_variation=None,
            median_interval_days=None,
            interval_variation=None,
            first_seen=pd.Timestamp("2026-01-01"),
            last_seen=pd.Timestamp("2026-01-01"),
            reason="Netflix has only 1 transaction.",
            transaction_ids=["z"],
        ),
    ]

    assert recurring_payment_insights(results) == []
