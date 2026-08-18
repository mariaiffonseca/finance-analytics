package com.mariafonseca.financeanalytics.features.overview.presentation

import com.mariafonseca.financeanalytics.core.common.Money
import com.mariafonseca.financeanalytics.core.common.toMajorUnits
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Client-side aggregation of already-imported local [Transaction]s into the
 * Overview dashboard's headline numbers (PR-015 §6, design system §16). This
 * is deliberately computed from local data rather than the Analytics API:
 * it is a straightforward sum/group-by over data already on the device, so
 * the dashboard's core numbers stay available even when the API is
 * unavailable (docs/project/01_PRODUCT_PRINCIPLES.md "Offline First",
 * PR-014 §8's local-first behaviour). Only the Top/More Insights section
 * needs the Insights Engine, since generating insights is genuinely
 * server-side analytical work.
 */
data class MonthlyOverview(
    val monthLabel: String,
    val totalSpent: Double,
    val totalIncome: Double,
    val totalSaved: Double,
    val categoryBreakdown: List<CategorySlice>,
    val monthlyTrend: List<MonthlyTrendPoint>,
)

data class CategorySlice(val category: String, val amount: Double, val share: Double)

data class MonthlyTrendPoint(val monthLabel: String, val totalSpent: Double)

private const val TREND_MONTHS = 6
private const val UNCATEGORISED_LABEL = "Uncategorised"

/**
 * Builds the dashboard for the most recent calendar month present in
 * [this], or `null` when there is no local data yet. Only expense
 * transactions (negative [Money.minorUnits]) drive spending/category/trend
 * figures; income transactions only contribute to [MonthlyOverview.totalIncome].
 *
 * Deliberately does not compute a "vs last month" percentage: the latest
 * month is frequently a partial month (the CSV export can end mid-month),
 * so naively comparing full-month totals would compare non-comparable
 * periods — exactly what PR-012 §8 and `insights.periods.build_period_comparison`
 * exist to avoid on the Python side. Reimplementing that comparable-window
 * logic here would duplicate Python analytical logic in Kotlin, which
 * PR-014's Critical Rule forbids; the API's `spending_trend` insight
 * already reports this correctly (see the Overview screen's Top Insight).
 */
fun List<Transaction>.toMonthlyOverview(): MonthlyOverview? {
    if (isEmpty()) return null

    val byMonth = groupBy { YearMonth.from(it.date) }
    val latestMonth = byMonth.keys.max()
    val currentMonthTransactions = byMonth.getValue(latestMonth)

    return MonthlyOverview(
        monthLabel = latestMonth.label(),
        totalSpent = currentMonthTransactions.totalExpenses(),
        totalIncome = currentMonthTransactions.totalIncome(),
        totalSaved = currentMonthTransactions.totalIncome() - currentMonthTransactions.totalExpenses(),
        categoryBreakdown = currentMonthTransactions.categoryBreakdown(),
        monthlyTrend = byMonth.keys.sorted().takeLast(TREND_MONTHS).map { month ->
            MonthlyTrendPoint(
                monthLabel = month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                totalSpent = byMonth.getValue(month).totalExpenses(),
            )
        },
    )
}

private fun List<Transaction>.totalExpenses(): Double =
    filter { it.amount.minorUnits < 0 }.sumOf { -it.amount.toMajorUnits() }

private fun List<Transaction>.totalIncome(): Double =
    filter { it.amount.minorUnits > 0 }.sumOf { it.amount.toMajorUnits() }

private fun List<Transaction>.categoryBreakdown(): List<CategorySlice> {
    val amountByCategory = filter { it.amount.minorUnits < 0 }
        .groupBy { it.category?.takeIf(String::isNotBlank) ?: UNCATEGORISED_LABEL }
        .mapValues { (_, transactions) -> transactions.sumOf { -it.amount.toMajorUnits() } }
        .filterValues { it > 0.0 }

    val total = amountByCategory.values.sum()
    if (total <= 0.0) return emptyList()

    return amountByCategory.entries
        .sortedByDescending { it.value }
        .map { (category, amount) -> CategorySlice(category, amount, amount / total) }
}

private fun YearMonth.label(): String =
    "${month.getDisplayName(TextStyle.FULL, Locale.getDefault())} $year"
