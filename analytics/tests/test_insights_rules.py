import pandas as pd

from finance_analytics.insights.models import IMPORTANT, NOTICE
from finance_analytics.insights.rules import (
    category_change_insights,
    income_expense_change_insights,
    savings_rate_change_insights,
    spending_trend_insights,
)


def _row(id_, date, amount, category="Food & Dining"):
    return {"id": id_, "date": pd.Timestamp(date), "amount": amount, "category": category}


#: February 2026 has 28 days; the last slot is always day 28 so
#: `_full_month_frame` always produces a *full*-month current period,
#: however many February transactions are requested.
_JAN_DAY_POOL = [5, 10, 15, 20, 25]
_FEB_DAY_POOL = [3, 9, 15, 21, 28]


def _full_month_frame(jan_amounts, feb_amounts, category="Food & Dining"):
    """Both windows land in full calendar months (February's last
    transaction is always on day 28, its actual last day) so results are
    not truncated by a comparable-partial-period day cutoff."""
    jan_days = _JAN_DAY_POOL[: len(jan_amounts)]
    feb_days = _FEB_DAY_POOL[-len(feb_amounts) :] if feb_amounts else []
    rows = [
        _row(f"j{i}", f"2026-01-{d:02d}", a, category)
        for i, (d, a) in enumerate(zip(jan_days, jan_amounts))
    ] + [
        _row(f"f{i}", f"2026-02-{d:02d}", a, category)
        for i, (d, a) in enumerate(zip(feb_days, feb_amounts))
    ]
    return pd.DataFrame(rows)


# --- Spending trend ----------------------------------------------------


def test_spending_trend_insufficient_history_returns_no_insight():
    frame = pd.DataFrame([_row("a", "2026-01-05", -20.0)])

    assert spending_trend_insights(frame) == []


def test_spending_trend_no_meaningful_change_returns_no_insight():
    frame = _full_month_frame([-20.0, -20.0, -20.0], [-20.5, -20.5, -20.5])

    assert spending_trend_insights(frame) == []


def test_spending_trend_increase_is_detected():
    frame = _full_month_frame([-20.0, -20.0], [-40.0, -40.0])

    insights = spending_trend_insights(frame)

    assert len(insights) == 1
    insight = insights[0]
    assert insight.title == "Spending increased"
    assert insight.metadata["change_percent"] == 100.0
    assert insight.metadata["comparison_kind"] == "full_month"
    assert insight.severity == IMPORTANT  # 100% is well past the 4x-threshold band


def test_spending_trend_decrease_is_detected():
    frame = _full_month_frame([-40.0, -40.0], [-20.0, -20.0])

    insights = spending_trend_insights(frame)

    assert len(insights) == 1
    assert insights[0].title == "Spending decreased"
    assert insights[0].metadata["change_percent"] == -50.0


def test_spending_trend_severity_escalates_with_magnitude():
    modest = spending_trend_insights(_full_month_frame([-100.0], [-140.0]))[0]  # +40%
    large = spending_trend_insights(_full_month_frame([-100.0], [-500.0]))[0]  # +400%

    assert modest.severity == NOTICE
    assert large.severity == IMPORTANT


def test_spending_trend_suppressed_when_previous_window_has_no_spend():
    frame = _full_month_frame([], [-40.0, -40.0])

    assert spending_trend_insights(frame) == []


def test_spending_trend_is_deterministic():
    frame = _full_month_frame([-20.0, -20.0], [-40.0, -40.0])

    assert spending_trend_insights(frame) == spending_trend_insights(frame)


# --- Category change -----------------------------------------------------


def test_category_change_increase_is_detected():
    frame = pd.DataFrame(
        [
            _row("j1", "2026-01-05", -20.0, "Groceries"),
            _row("j2", "2026-01-12", -20.0, "Groceries"),
            _row("f1", "2026-02-04", -40.0, "Groceries"),
            _row("f2", "2026-02-28", -40.0, "Groceries"),
        ]
    )

    insights = category_change_insights(frame)

    assert len(insights) == 1
    assert insights[0].category == "Groceries"
    assert insights[0].title == "Groceries spending increased"
    assert insights[0].metadata["change_percent"] == 100.0


def test_category_change_stable_category_produces_no_insight():
    frame = pd.DataFrame(
        [
            _row("j1", "2026-01-05", -20.0, "Groceries"),
            _row("j2", "2026-01-12", -20.0, "Groceries"),
            _row("f1", "2026-02-04", -21.0, "Groceries"),
            _row("f2", "2026-02-28", -20.0, "Groceries"),
        ]
    )

    assert category_change_insights(frame) == []


def test_category_change_insufficient_transactions_in_one_window_is_suppressed():
    frame = pd.DataFrame(
        [
            _row("j1", "2026-01-05", -20.0, "Transport"),  # only 1 in January
            _row("f1", "2026-02-04", -40.0, "Transport"),
            _row("f2", "2026-02-28", -40.0, "Transport"),
        ]
    )

    assert category_change_insights(frame) == []


def test_category_change_present_in_only_one_period_is_suppressed():
    frame = pd.DataFrame(
        [
            _row("j1", "2026-01-05", -20.0, "Groceries"),
            _row("j2", "2026-01-12", -20.0, "Groceries"),
            _row("f1", "2026-02-04", -40.0, "Health"),  # a different category in February
            _row("f2", "2026-02-28", -40.0, "Health"),
        ]
    )

    assert category_change_insights(frame) == []


def test_category_change_covers_multiple_categories_independently():
    frame = pd.DataFrame(
        [
            _row("j1", "2026-01-05", -20.0, "Groceries"),
            _row("j2", "2026-01-12", -20.0, "Groceries"),
            _row("j3", "2026-01-13", -10.0, "Health"),
            _row("j4", "2026-01-14", -10.0, "Health"),
            _row("f1", "2026-02-04", -40.0, "Groceries"),  # up
            _row("f2", "2026-02-05", -40.0, "Groceries"),
            _row("f3", "2026-02-06", -10.0, "Health"),  # stable
            _row("f4", "2026-02-28", -10.0, "Health"),
        ]
    )

    insights = category_change_insights(frame)

    categories = {insight.category for insight in insights}
    assert categories == {"Groceries"}


# --- Income / expense change ----------------------------------------------


def _row_with_income(rows_expenses, income_rows):
    return pd.DataFrame(rows_expenses + income_rows)


def test_income_expense_change_fires_when_expenses_rise_and_income_is_stable():
    frame = _row_with_income(
        [
            _row("j1", "2026-01-05", -20.0),
            _row("j2", "2026-01-12", -20.0),
            _row("f1", "2026-02-04", -40.0),
            _row("f2", "2026-02-28", -40.0),
        ],
        [
            {
                "id": "ji",
                "date": pd.Timestamp("2026-01-28"),
                "amount": 2000.0,
                "category": "Income",
            },
            {
                "id": "fi",
                "date": pd.Timestamp("2026-02-27"),
                "amount": 2010.0,
                "category": "Income",
            },
        ],
    )

    insights = income_expense_change_insights(frame)

    assert len(insights) == 1
    assert "income remained stable" in insights[0].description
    assert "expenses increased" in insights[0].description


def test_income_expense_change_suppressed_when_moving_the_same_direction():
    # Both expenses and income double — not a divergence, already covered
    # by spending_trend_insights.
    frame = _row_with_income(
        [
            _row("j1", "2026-01-05", -20.0),
            _row("j2", "2026-01-12", -20.0),
            _row("f1", "2026-02-04", -40.0),
            _row("f2", "2026-02-28", -40.0),
        ],
        [
            {
                "id": "ji",
                "date": pd.Timestamp("2026-01-28"),
                "amount": 1000.0,
                "category": "Income",
            },
            {
                "id": "fi",
                "date": pd.Timestamp("2026-02-27"),
                "amount": 2000.0,
                "category": "Income",
            },
        ],
    )

    assert income_expense_change_insights(frame) == []


def test_income_expense_change_suppressed_without_previous_income():
    frame = _row_with_income(
        [
            _row("j1", "2026-01-05", -20.0),
            _row("j2", "2026-01-12", -20.0),
            _row("f1", "2026-02-04", -40.0),
            _row("f2", "2026-02-28", -40.0),
        ],
        [{"id": "fi", "date": pd.Timestamp("2026-02-27"), "amount": 2000.0, "category": "Income"}],
    )

    assert income_expense_change_insights(frame) == []


# --- Savings rate change ---------------------------------------------------


def test_savings_rate_change_decrease_is_detected():
    frame = _row_with_income(
        [
            _row("j1", "2026-01-05", -20.0),
            _row("j2", "2026-01-12", -20.0),
            _row("f1", "2026-02-04", -80.0),
            _row("f2", "2026-02-28", -80.0),
        ],
        [
            {
                "id": "ji",
                "date": pd.Timestamp("2026-01-28"),
                "amount": 1000.0,
                "category": "Income",
            },
            {
                "id": "fi",
                "date": pd.Timestamp("2026-02-27"),
                "amount": 1000.0,
                "category": "Income",
            },
        ],
    )

    insights = savings_rate_change_insights(frame)

    assert len(insights) == 1
    assert insights[0].title == "Savings rate decreased"
    assert insights[0].metadata["previous_value"] == 96.0
    assert insights[0].metadata["current_value"] == 84.0


def test_savings_rate_change_suppressed_without_income_in_either_window():
    frame = pd.DataFrame(
        [
            _row("j1", "2026-01-05", -20.0),
            _row("f1", "2026-02-28", -80.0),
        ]
    )

    assert savings_rate_change_insights(frame) == []


def test_savings_rate_change_small_move_is_not_reported():
    frame = _row_with_income(
        [
            _row("j1", "2026-01-05", -20.0),
            _row("j2", "2026-01-12", -20.0),
            _row("f1", "2026-02-04", -21.0),
            _row("f2", "2026-02-28", -21.0),
        ],
        [
            {
                "id": "ji",
                "date": pd.Timestamp("2026-01-28"),
                "amount": 1000.0,
                "category": "Income",
            },
            {
                "id": "fi",
                "date": pd.Timestamp("2026-02-27"),
                "amount": 1000.0,
                "category": "Income",
            },
        ],
    )

    assert savings_rate_change_insights(frame) == []
