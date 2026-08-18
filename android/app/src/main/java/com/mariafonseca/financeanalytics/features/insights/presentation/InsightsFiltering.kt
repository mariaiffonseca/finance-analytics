package com.mariafonseca.financeanalytics.features.insights.presentation

import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsResult
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsSummary
import com.mariafonseca.financeanalytics.core.analytics.model.Anomaly
import com.mariafonseca.financeanalytics.core.analytics.model.Insight
import com.mariafonseca.financeanalytics.core.analytics.model.RecurringTransaction

// Mirrors finance_analytics.insights.models' controlled type vocabulary
// (analytics/src/finance_analytics/insights/models.py) — the API contract
// (InsightDto.type) is a plain string, not an enum, so these are matched by
// value rather than imported. unusual_transaction/recurring_payment are
// deliberately not referenced here: those insight rows are only shown in
// the Recent/all view (see viewFor's doc comment) — the Anomalies/Recurring
// filters show the dedicated section instead, to avoid double-showing the
// same flagged transaction.
private const val TYPE_SPENDING_TREND = "spending_trend"
private const val TYPE_CATEGORY_CHANGE = "category_change"
private const val TYPE_INCOME_EXPENSE_CHANGE = "income_expense_change"
private const val TYPE_SAVINGS_RATE_CHANGE = "savings_rate_change"

private val TRENDS_TYPES = setOf(TYPE_SPENDING_TREND, TYPE_INCOME_EXPENSE_CHANGE, TYPE_SAVINGS_RATE_CHANGE)

/**
 * One filtered view of an [AnalyticsResult] (PR-015 §7). A `null` section
 * means that section is not part of the selected filter and should not be
 * rendered at all; an empty (non-null) list means the section applies but
 * has no matching data, which the UI renders as a section-specific empty
 * state.
 *
 * Filtering operates on the structured `type`/data fields of the analytics
 * result, never on rendered text, and never triggers a new analytics
 * request — the underlying [AnalyticsResult] is already in memory.
 */
data class InsightsView(
    val summary: AnalyticsSummary,
    val insights: List<Insight>?,
    val anomalies: List<Anomaly>?,
    val recurring: List<RecurringTransaction>?,
)

/**
 * Applies [filter] to this result. [InsightFilter.RECENT] is the "all"
 * filter (docs/project/05_DESIGN_SYSTEM.md §17 lists it first, in the
 * position PR-015's example calls "All") and shows every section
 * unfiltered. Every other filter narrows to the one data category it
 * names:
 *
 * - Spending -> insights about category-level spending (`category_change`).
 * - Trends -> insights about spending/income trends over time
 *   (`spending_trend`, `income_expense_change`, `savings_rate_change`).
 * - Anomalies -> the anomalies section itself, not the derived
 *   `unusual_transaction` insight rows, to avoid showing the same flagged
 *   transaction twice.
 * - Recurring -> the recurring section itself, for the same reason.
 */
fun AnalyticsResult.viewFor(filter: InsightFilter): InsightsView = when (filter) {
    InsightFilter.RECENT -> InsightsView(summary, insights, anomalies, recurring)
    InsightFilter.SPENDING -> InsightsView(
        summary = summary,
        insights = insights.filter { it.type == TYPE_CATEGORY_CHANGE },
        anomalies = null,
        recurring = null,
    )
    InsightFilter.TRENDS -> InsightsView(
        summary = summary,
        insights = insights.filter { it.type in TRENDS_TYPES },
        anomalies = null,
        recurring = null,
    )
    InsightFilter.ANOMALIES -> InsightsView(
        summary = summary,
        insights = null,
        anomalies = anomalies,
        recurring = null,
    )
    InsightFilter.RECURRING -> InsightsView(
        summary = summary,
        insights = null,
        anomalies = null,
        recurring = recurring,
    )
}
