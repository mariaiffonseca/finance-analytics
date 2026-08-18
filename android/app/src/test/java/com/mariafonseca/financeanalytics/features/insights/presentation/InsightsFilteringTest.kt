package com.mariafonseca.financeanalytics.features.insights.presentation

import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsResult
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsSummary
import com.mariafonseca.financeanalytics.core.analytics.model.Anomaly
import com.mariafonseca.financeanalytics.core.analytics.model.Insight
import com.mariafonseca.financeanalytics.core.analytics.model.RecurringTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class InsightsFilteringTest {

    private val summary = AnalyticsSummary(
        transactionCount = 1,
        incomeCount = 0,
        expenseCount = 1,
        totalIncome = 0.0,
        totalExpenses = -100.0,
        netSavings = -100.0,
        dateRangeStart = null,
        dateRangeEnd = null,
        uniqueMerchantCount = 1,
        uniqueCategoryCount = 1,
    )

    private fun insight(type: String, id: String = type) = Insight(
        id = id,
        type = type,
        title = type,
        description = type,
        severity = "INFO",
        confidence = 0.5,
        relatedTransactionIds = emptyList(),
        category = null,
        merchant = null,
        amount = null,
        comparisonPeriod = null,
    )

    private val anomaly = Anomaly(
        transactionId = "1",
        anomalyScore = 3.0,
        isAnomaly = true,
        method = "z-score",
        reason = "unusual",
    )

    private val recurring = RecurringTransaction(
        merchant = "Netflix",
        currency = "EUR",
        isRecurring = true,
        classification = "Recurring",
        confidenceScore = 0.9,
        frequency = "monthly",
        occurrences = 4,
        medianAmount = -15.99,
        amountVariation = 0.0,
        medianIntervalDays = 30.0,
        intervalVariation = 0.0,
        firstSeen = LocalDate.of(2026, 1, 1),
        lastSeen = LocalDate.of(2026, 4, 1),
        reason = "recurring",
        transactionIds = listOf("1", "2", "3", "4"),
    )

    private val result = AnalyticsResult(
        summary = summary,
        insights = listOf(
            insight("spending_trend"),
            insight("category_change"),
            insight("income_expense_change"),
            insight("savings_rate_change"),
            insight("unusual_transaction"),
            insight("recurring_payment"),
        ),
        anomalies = listOf(anomaly),
        recurring = listOf(recurring),
    )

    @Test
    fun `recent filter shows every section unfiltered`() {
        val view = result.viewFor(InsightFilter.RECENT)

        assertEquals(result.insights, view.insights)
        assertEquals(result.anomalies, view.anomalies)
        assertEquals(result.recurring, view.recurring)
    }

    @Test
    fun `spending filter shows only category change insights and hides other sections`() {
        val view = result.viewFor(InsightFilter.SPENDING)

        assertEquals(listOf("category_change"), view.insights?.map { it.type })
        assertNull(view.anomalies)
        assertNull(view.recurring)
    }

    @Test
    fun `trends filter shows spending, income-expense and savings-rate insights`() {
        val view = result.viewFor(InsightFilter.TRENDS)

        assertEquals(
            setOf("spending_trend", "income_expense_change", "savings_rate_change"),
            view.insights?.map { it.type }?.toSet(),
        )
        assertNull(view.anomalies)
        assertNull(view.recurring)
    }

    @Test
    fun `anomalies filter shows only the anomalies section`() {
        val view = result.viewFor(InsightFilter.ANOMALIES)

        assertNull(view.insights)
        assertEquals(result.anomalies, view.anomalies)
        assertNull(view.recurring)
    }

    @Test
    fun `recurring filter shows only the recurring section`() {
        val view = result.viewFor(InsightFilter.RECURRING)

        assertNull(view.insights)
        assertNull(view.anomalies)
        assertEquals(result.recurring, view.recurring)
    }

    @Test
    fun `spending filter with no matching insights yields an empty but non-null list`() {
        val noSpendingResult = result.copy(insights = listOf(insight("unusual_transaction")))

        val view = noSpendingResult.viewFor(InsightFilter.SPENDING)

        assertEquals(emptyList<Insight>(), view.insights)
    }

    @Test
    fun `every filter preserves the summary unchanged`() {
        InsightFilter.entries.forEach { filter ->
            assertEquals(summary, result.viewFor(filter).summary)
        }
    }
}
