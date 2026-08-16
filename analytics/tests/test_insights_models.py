import json

from finance_analytics.insights.models import IMPORTANT, INSIGHT_TYPES, SEVERITIES, Insight


def test_to_dict_is_json_serialisable_and_matches_fields():
    insight = Insight(
        id="spending_trend:2026-03",
        type="spending_trend",
        title="Spending increased",
        description="Your spending increased 22% compared with last month.",
        severity=IMPORTANT,
        confidence=0.8,
        related_transaction_ids=("1", "2"),
        metadata={"change_percent": 22.0},
        category=None,
        merchant=None,
        amount=123.45,
        comparison_period="2026-03",
    )

    payload = insight.to_dict()
    encoded = json.dumps(payload)  # PR-012 §16: must be a valid future API response body
    decoded = json.loads(encoded)

    assert decoded["id"] == "spending_trend:2026-03"
    assert decoded["related_transaction_ids"] == ["1", "2"]
    assert decoded["metadata"] == {"change_percent": 22.0}
    assert decoded["confidence"] == 0.8


def test_default_related_ids_and_metadata_are_empty_not_shared_mutable_state():
    a = Insight(id="a", type="x", title="t", description="d", severity=IMPORTANT, confidence=0.5)
    b = Insight(id="b", type="x", title="t", description="d", severity=IMPORTANT, confidence=0.5)

    a.metadata["mutated"] = True

    assert "mutated" not in b.metadata
    assert a.related_transaction_ids == ()


def test_severity_and_type_vocabularies_are_small_and_controlled():
    assert set(SEVERITIES) == {"INFO", "NOTICE", "IMPORTANT"}
    assert len(INSIGHT_TYPES) == 6
