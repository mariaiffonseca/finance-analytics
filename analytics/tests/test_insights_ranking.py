from finance_analytics.insights.models import IMPORTANT, INFO, NOTICE, Insight
from finance_analytics.insights.ranking import rank_insights


def _insight(id_, severity, confidence, comparison_period=None, last_seen=None):
    return Insight(
        id=id_,
        type="spending_trend",
        title="t",
        description="d",
        severity=severity,
        confidence=confidence,
        comparison_period=comparison_period,
        metadata={"last_seen": last_seen} if last_seen else {},
    )


def test_higher_severity_ranks_above_lower_severity_regardless_of_confidence():
    low_severity_high_confidence = _insight("a", INFO, 0.95)
    high_severity_low_confidence = _insight("b", IMPORTANT, 0.1)

    ranked = rank_insights([low_severity_high_confidence, high_severity_low_confidence])

    assert [i.id for i in ranked] == ["b", "a"]


def test_confidence_breaks_ties_within_the_same_severity():
    lower = _insight("a", NOTICE, 0.4)
    higher = _insight("b", NOTICE, 0.9)

    ranked = rank_insights([lower, higher])

    assert [i.id for i in ranked] == ["b", "a"]


def test_recency_breaks_ties_within_same_severity_and_confidence():
    older = _insight("a", NOTICE, 0.7, comparison_period="2026-01")
    newer = _insight("b", NOTICE, 0.7, comparison_period="2026-03")

    ranked = rank_insights([older, newer])

    assert [i.id for i in ranked] == ["b", "a"]


def test_id_is_the_final_deterministic_tiebreaker():
    first = _insight("aaa", NOTICE, 0.7)
    second = _insight("zzz", NOTICE, 0.7)

    ranked = rank_insights([second, first])

    assert [i.id for i in ranked] == ["aaa", "zzz"]


def test_ranking_is_deterministic_regardless_of_input_order():
    insights = [
        _insight("c", INFO, 0.5),
        _insight("a", IMPORTANT, 0.9),
        _insight("b", NOTICE, 0.6),
    ]

    first_run = rank_insights(insights)
    second_run = rank_insights(list(reversed(insights)))

    assert [i.id for i in first_run] == [i.id for i in second_run] == ["a", "b", "c"]


def test_ranking_an_empty_list_returns_an_empty_list():
    assert rank_insights([]) == []
