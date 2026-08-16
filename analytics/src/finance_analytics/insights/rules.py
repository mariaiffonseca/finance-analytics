"""Deterministic period-comparison insight rules (PR-012 §§2-3, 8).

Every rule here follows the same shape (PR-012, Critical Principle):

    Evidence (a comparable current-vs-previous window, `periods.py`)
        -> Analytical rule (a documented, fixed threshold)
        -> Insight, or suppression when evidence is insufficient

None of the thresholds below are statistically fitted — like every other
threshold in this codebase (`anomalies.detector.ANOMALY_Z_THRESHOLD`,
`recurring.detector.RECURRING_CONFIDENCE_THRESHOLD`), they are evidence-
motivated and documented at their definition, not tuned against labelled
outcomes (none exist for this product). PR-008's EDA never computed a real
month-over-month change itself — its two observed months are not
comparable — so these thresholds are justified by general
personal-finance-analytics practice and by the sample-size cautions the EDA
*did* establish (most categories have 1-4 transactions), not by a number
lifted from the EDA.

This module never reimplements anomaly detection or recurring-transaction
classification — see `conversions.py` for those.
"""

from __future__ import annotations

import pandas as pd

from finance_analytics.insights.models import (
    CATEGORY_CHANGE,
    IMPORTANT,
    INCOME_EXPENSE_CHANGE,
    INFO,
    NOTICE,
    SAVINGS_RATE_CHANGE,
    SPENDING_TREND,
    Insight,
)
from finance_analytics.insights.periods import (
    build_period_comparison,
    comparison_confidence,
    windowed_frames,
)

#: A total-spend change below this is ordinary month-to-month noise, not a
#: trend worth surfacing. 15% is a conservative "clearly more than noise"
#: bar for a whole-account comparison.
SPENDING_TREND_THRESHOLD = 0.15

#: Category-level totals are smaller and noisier than the account-wide
#: total (PR-008's EDA: most categories here have only 1-4 transactions),
#: so this sits above `SPENDING_TREND_THRESHOLD` — a category swing has to
#: be larger before it's trusted as a real change rather than one
#: different purchase.
CATEGORY_CHANGE_THRESHOLD = 0.25

#: A category needs at least this many transactions in *both* comparison
#: windows before a change is reported. Mirrors
#: `anomalies.detector.MIN_MERCHANT_HISTORY`'s "two is the floor for a
#: baseline to mean anything at all" reasoning — below this, a "change" is
#: just one different purchase, not a pattern. On this project's own
#: 18-row fixture, every category has exactly one transaction per
#: comparable-partial window, so this floor suppresses all real-data
#: category-change insights — see `notebooks/06_insights_engine.ipynb`.
MIN_CATEGORY_TRANSACTIONS = 2

#: An income change smaller than this is described as "stable" rather than
#: directional, in `income_expense_change_insights`.
INCOME_STABLE_THRESHOLD = 0.05

#: A savings-rate move smaller than this many percentage points is not
#: reported — a "clearly more than a rounding fluctuation" bar for a ratio
#: metric.
SAVINGS_RATE_CHANGE_THRESHOLD_PP = 0.10

#: Severity escalates at multiples of whichever base threshold a rule uses,
#: so escalation is proportional to that rule's own notion of "meaningful"
#: rather than an independently chosen absolute number.
_NOTICE_MULTIPLE = 2.0
_IMPORTANT_MULTIPLE = 4.0


def _severity_for(change_magnitude: float, threshold: float) -> str:
    if change_magnitude >= threshold * _IMPORTANT_MULTIPLE:
        return IMPORTANT
    if change_magnitude >= threshold * _NOTICE_MULTIPLE:
        return NOTICE
    return INFO


def _expense_total(frame: pd.DataFrame, amount_column: str) -> float:
    expenses = frame.loc[frame[amount_column] < 0, amount_column]
    return float(-expenses.sum()) if not expenses.empty else 0.0


def _income_total(frame: pd.DataFrame, amount_column: str) -> float:
    income = frame.loc[frame[amount_column] > 0, amount_column]
    return float(income.sum()) if not income.empty else 0.0


def _related_ids(*groups: pd.Series) -> tuple[str, ...]:
    return tuple(str(v) for v in pd.concat(groups))


def spending_trend_insights(
    frame: pd.DataFrame,
    date_column: str = "date",
    amount_column: str = "amount",
    id_column: str = "id",
) -> list[Insight]:
    """PR-012 §2, Spending Trend: total expenses, current comparison window
    vs previous.

    Suppressed (returns `[]`) when there are fewer than two comparable
    months (`periods.build_period_comparison`), when the previous window
    had no expenses to compare against, or when the change is within
    `SPENDING_TREND_THRESHOLD` of flat.
    """
    comparison = build_period_comparison(frame, date_column=date_column)
    if comparison is None:
        return []

    current, previous = windowed_frames(frame, comparison, date_column=date_column)
    current_spend = _expense_total(current, amount_column)
    previous_spend = _expense_total(previous, amount_column)
    if previous_spend <= 0:
        return []

    change = (current_spend - previous_spend) / previous_spend
    if abs(change) < SPENDING_TREND_THRESHOLD:
        return []

    direction = "increased" if change > 0 else "decreased"
    severity = _severity_for(abs(change), SPENDING_TREND_THRESHOLD)
    confidence = comparison_confidence(comparison, len(current) + len(previous))
    related_ids = _related_ids(
        current.loc[current[amount_column] < 0, id_column],
        previous.loc[previous[amount_column] < 0, id_column],
    )

    return [
        Insight(
            id=f"spending_trend:{comparison.current_period}",
            type=SPENDING_TREND,
            title=f"Spending {direction}",
            description=(
                f"Your spending {direction} {abs(change) * 100:.0f}% compared with "
                f"{comparison.label}."
            ),
            severity=severity,
            confidence=confidence,
            related_transaction_ids=related_ids,
            amount=round(current_spend, 2),
            comparison_period=comparison.current_period,
            metadata={
                "current_value": round(current_spend, 2),
                "previous_value": round(previous_spend, 2),
                "change_percent": round(change * 100, 1),
                "comparison_period": comparison.current_period,
                "comparison_kind": comparison.comparison_kind,
                "day_cutoff": comparison.day_cutoff,
                "threshold": SPENDING_TREND_THRESHOLD * 100,
                "source_feature": "insights.rules.spending_trend_insights",
            },
        )
    ]


def category_change_insights(
    frame: pd.DataFrame,
    date_column: str = "date",
    amount_column: str = "amount",
    category_column: str = "category",
    id_column: str = "id",
) -> list[Insight]:
    """PR-012 §2, Category Change: per-category expense total, current
    window vs previous.

    Only categories present with at least `MIN_CATEGORY_TRANSACTIONS`
    transactions in *both* windows are considered; a category present in
    only one window has no percent change to report (a new/discontinued
    category, not a "change" this rule describes).
    """
    comparison = build_period_comparison(frame, date_column=date_column)
    if comparison is None:
        return []

    current, previous = windowed_frames(frame, comparison, date_column=date_column)
    current_expenses = current[current[amount_column] < 0]
    previous_expenses = previous[previous[amount_column] < 0]

    current_by_category = current_expenses.groupby(category_column)
    previous_by_category = previous_expenses.groupby(category_column)
    shared_categories = set(current_by_category.groups) & set(previous_by_category.groups)

    insights = []
    for category in sorted(shared_categories):
        current_group = current_by_category.get_group(category)
        previous_group = previous_by_category.get_group(category)
        if (
            len(current_group) < MIN_CATEGORY_TRANSACTIONS
            or len(previous_group) < MIN_CATEGORY_TRANSACTIONS
        ):
            continue

        current_spend = float(-current_group[amount_column].sum())
        previous_spend = float(-previous_group[amount_column].sum())
        if previous_spend <= 0:
            continue

        change = (current_spend - previous_spend) / previous_spend
        if abs(change) < CATEGORY_CHANGE_THRESHOLD:
            continue

        direction = "increased" if change > 0 else "decreased"
        severity = _severity_for(abs(change), CATEGORY_CHANGE_THRESHOLD)
        confidence = comparison_confidence(comparison, len(current_group) + len(previous_group))
        related_ids = _related_ids(current_group[id_column], previous_group[id_column])

        insights.append(
            Insight(
                id=f"category_change:{category}:{comparison.current_period}",
                type=CATEGORY_CHANGE,
                title=f"{category} spending {direction}",
                description=(
                    f"Your {category} spending {direction} {abs(change) * 100:.0f}% "
                    f"compared with {comparison.label}."
                ),
                severity=severity,
                confidence=confidence,
                related_transaction_ids=related_ids,
                category=str(category),
                amount=round(current_spend, 2),
                comparison_period=comparison.current_period,
                metadata={
                    "current_value": round(current_spend, 2),
                    "previous_value": round(previous_spend, 2),
                    "change_percent": round(change * 100, 1),
                    "comparison_period": comparison.current_period,
                    "comparison_kind": comparison.comparison_kind,
                    "day_cutoff": comparison.day_cutoff,
                    "threshold": CATEGORY_CHANGE_THRESHOLD * 100,
                    "source_feature": "insights.rules.category_change_insights",
                },
            )
        )
    return insights


def income_expense_change_insights(
    frame: pd.DataFrame,
    date_column: str = "date",
    amount_column: str = "amount",
    id_column: str = "id",
) -> list[Insight]:
    """PR-012 §2, Income / Expense Change: fires only when expenses move
    meaningfully *and* income does not move the same way by a meaningful
    degree.

    A same-direction move (both up, or both down) is not reported here — it
    is already covered by `spending_trend_insights` and is not a
    "divergence" between the two. Suppressed entirely when either window
    has no income at all: a percent change against a zero baseline is
    undefined, not merely large.
    """
    comparison = build_period_comparison(frame, date_column=date_column)
    if comparison is None:
        return []

    current, previous = windowed_frames(frame, comparison, date_column=date_column)
    current_expense = _expense_total(current, amount_column)
    previous_expense = _expense_total(previous, amount_column)
    current_income = _income_total(current, amount_column)
    previous_income = _income_total(previous, amount_column)

    if previous_expense <= 0 or previous_income <= 0 or current_income <= 0:
        return []

    expense_change = (current_expense - previous_expense) / previous_expense
    income_change = (current_income - previous_income) / previous_income

    if abs(expense_change) < SPENDING_TREND_THRESHOLD:
        return []

    income_stable = abs(income_change) < INCOME_STABLE_THRESHOLD
    same_direction_move = not income_stable and (income_change > 0) == (expense_change > 0)
    if same_direction_move:
        return []

    expense_direction = "increased" if expense_change > 0 else "decreased"
    if income_stable:
        income_phrase = "income remained stable"
    else:
        income_direction = "increased" if income_change > 0 else "decreased"
        income_phrase = f"income {income_direction} {abs(income_change) * 100:.0f}%"

    severity = _severity_for(abs(expense_change), SPENDING_TREND_THRESHOLD)
    confidence = comparison_confidence(comparison, len(current) + len(previous))
    related_ids = _related_ids(current[id_column], previous[id_column])

    return [
        Insight(
            id=f"income_expense_change:{comparison.current_period}",
            type=INCOME_EXPENSE_CHANGE,
            title="Expenses and income diverged",
            description=(
                f"Your expenses {expense_direction} {abs(expense_change) * 100:.0f}% while "
                f"{income_phrase}, compared with {comparison.label}."
            ),
            severity=severity,
            confidence=confidence,
            related_transaction_ids=related_ids,
            comparison_period=comparison.current_period,
            metadata={
                "current_value": round(current_expense, 2),
                "previous_value": round(previous_expense, 2),
                "change_percent": round(expense_change * 100, 1),
                "current_income": round(current_income, 2),
                "previous_income": round(previous_income, 2),
                "income_change_percent": round(income_change * 100, 1),
                "comparison_period": comparison.current_period,
                "comparison_kind": comparison.comparison_kind,
                "day_cutoff": comparison.day_cutoff,
                "threshold": SPENDING_TREND_THRESHOLD * 100,
                "source_feature": "insights.rules.income_expense_change_insights",
            },
        )
    ]


def savings_rate_change_insights(
    frame: pd.DataFrame,
    date_column: str = "date",
    amount_column: str = "amount",
    id_column: str = "id",
) -> list[Insight]:
    """PR-012 §2, Savings Rate Change: `(income - expenses) / income`,
    current window vs previous, expressed as a percentage-point change.

    Suppressed when either window has no income — a savings rate is
    undefined without one (division by zero), not merely low or unusual.
    Purely descriptive, no financial advice (PR-012 §15).
    """
    comparison = build_period_comparison(frame, date_column=date_column)
    if comparison is None:
        return []

    current, previous = windowed_frames(frame, comparison, date_column=date_column)
    current_income = _income_total(current, amount_column)
    previous_income = _income_total(previous, amount_column)
    if current_income <= 0 or previous_income <= 0:
        return []

    current_expense = _expense_total(current, amount_column)
    previous_expense = _expense_total(previous, amount_column)
    current_rate = (current_income - current_expense) / current_income
    previous_rate = (previous_income - previous_expense) / previous_income
    change_pp = current_rate - previous_rate

    if abs(change_pp) < SAVINGS_RATE_CHANGE_THRESHOLD_PP:
        return []

    direction = "increased" if change_pp > 0 else "decreased"
    severity = _severity_for(abs(change_pp), SAVINGS_RATE_CHANGE_THRESHOLD_PP)
    confidence = comparison_confidence(comparison, len(current) + len(previous))
    related_ids = _related_ids(current[id_column], previous[id_column])

    return [
        Insight(
            id=f"savings_rate_change:{comparison.current_period}",
            type=SAVINGS_RATE_CHANGE,
            title=f"Savings rate {direction}",
            description=(
                f"Your savings rate {direction} from {previous_rate * 100:.0f}% to "
                f"{current_rate * 100:.0f}% compared with {comparison.label}."
            ),
            severity=severity,
            confidence=confidence,
            related_transaction_ids=related_ids,
            comparison_period=comparison.current_period,
            metadata={
                "current_value": round(current_rate * 100, 1),
                "previous_value": round(previous_rate * 100, 1),
                "change_percent": round(change_pp * 100, 1),
                "comparison_period": comparison.current_period,
                "comparison_kind": comparison.comparison_kind,
                "day_cutoff": comparison.day_cutoff,
                "threshold": SAVINGS_RATE_CHANGE_THRESHOLD_PP * 100,
                "source_feature": "insights.rules.savings_rate_change_insights",
            },
        )
    ]
