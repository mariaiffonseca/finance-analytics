package com.mariafonseca.financeanalytics.core.analytics

import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsMetadataDto
import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsResponseDto
import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsSummaryDto
import com.mariafonseca.financeanalytics.core.analytics.dto.AnomalyDto
import com.mariafonseca.financeanalytics.core.analytics.dto.InsightDto
import com.mariafonseca.financeanalytics.core.analytics.dto.RecurringDto
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AnalyticsResponseMapperTest {

    private val summaryDto = AnalyticsSummaryDto(
        transactionCount = 10,
        incomeCount = 2,
        expenseCount = 8,
        totalIncome = 2000.0,
        totalExpenses = -1500.0,
        netSavings = 500.0,
        dateRangeStart = "2026-07-01",
        dateRangeEnd = "2026-07-31",
        uniqueMerchantCount = 5,
        uniqueCategoryCount = 3,
    )

    private val insightDto = InsightDto(
        id = "insight-1",
        type = "negative",
        title = "Restaurant spending increased",
        description = "Restaurant spending increased by 24% compared to last month.",
        severity = "warning",
        confidence = 0.87,
        relatedTransactionIds = listOf("1", "2"),
        metadata = JsonObject(emptyMap()),
        category = "Restaurants",
        merchant = null,
        amount = 420.0,
        comparisonPeriod = "last_month",
    )

    private val anomalyDto = AnomalyDto(
        transactionId = "9",
        anomalyScore = 3.2,
        isAnomaly = true,
        method = "z-score",
        reason = "Amount is far above the merchant's usual range.",
        referenceContext = JsonObject(emptyMap()),
    )

    private val recurringDto = RecurringDto(
        merchant = "Netflix",
        currency = "EUR",
        isRecurring = true,
        classification = "Recurring",
        confidenceScore = 0.95,
        frequency = "monthly",
        occurrences = 6,
        medianAmount = -12.99,
        amountVariation = 0.0,
        medianIntervalDays = 30.0,
        intervalVariation = 1.0,
        firstSeen = "2026-01-05",
        lastSeen = "2026-07-05",
        reason = "Consistent monthly charge.",
        transactionIds = listOf("3", "4", "5"),
    )

    private val metadataDto = AnalyticsMetadataDto(
        requestedTransactionCount = 10,
        processedTransactionCount = 10,
        duplicateRowCount = 0,
        duplicateIdCount = 0,
        invalidDateCount = 0,
        invalidAmountCount = 0,
        insightCount = 1,
        anomalyCount = 1,
        recurringCount = 1,
    )

    @Test
    fun `a full response maps every field into the domain result`() {
        val response = AnalyticsResponseDto(
            summary = summaryDto,
            insights = listOf(insightDto),
            anomalies = listOf(anomalyDto),
            recurring = listOf(recurringDto),
            metadata = metadataDto,
        )

        val result = response.toDomain()

        assertEquals(10, result.summary.transactionCount)
        assertEquals(-1500.0, result.summary.totalExpenses, 0.0001)
        assertEquals(LocalDate.of(2026, 7, 1), result.summary.dateRangeStart)
        assertEquals(LocalDate.of(2026, 7, 31), result.summary.dateRangeEnd)

        assertEquals(1, result.insights.size)
        val insight = result.insights.single()
        assertEquals("insight-1", insight.id)
        assertEquals("negative", insight.type)
        assertEquals("Restaurants", insight.category)
        assertNull(insight.merchant)
        assertEquals(420.0, insight.amount)
        assertEquals("last_month", insight.comparisonPeriod)

        assertEquals(1, result.anomalies.size)
        val anomaly = result.anomalies.single()
        assertEquals("9", anomaly.transactionId)
        assertEquals(3.2, anomaly.anomalyScore)
        assertTrue(anomaly.isAnomaly)

        assertEquals(1, result.recurring.size)
        val recurring = result.recurring.single()
        assertEquals("Netflix", recurring.merchant)
        assertEquals(LocalDate.of(2026, 1, 5), recurring.firstSeen)
        assertEquals(LocalDate.of(2026, 7, 5), recurring.lastSeen)
        assertEquals(listOf("3", "4", "5"), recurring.transactionIds)
    }

    @Test
    fun `missing optional summary date range maps to null`() {
        val summary = summaryDto.copy(dateRangeStart = null, dateRangeEnd = null)

        val domain = summary.toDomain()

        assertNull(domain.dateRangeStart)
        assertNull(domain.dateRangeEnd)
    }

    @Test
    fun `missing optional insight fields map to null`() {
        val insight = insightDto.copy(category = null, merchant = "Amazon", amount = null, comparisonPeriod = null)

        val domain = insight.toDomain()

        assertNull(domain.category)
        assertEquals("Amazon", domain.merchant)
        assertNull(domain.amount)
        assertNull(domain.comparisonPeriod)
    }

    @Test
    fun `missing optional anomaly score maps to null`() {
        val anomaly = anomalyDto.copy(anomalyScore = null)

        assertNull(anomaly.toDomain().anomalyScore)
    }

    @Test
    fun `missing optional recurring fields map to null`() {
        val recurring = recurringDto.copy(
            confidenceScore = null,
            medianAmount = null,
            amountVariation = null,
            medianIntervalDays = null,
            intervalVariation = null,
        )

        val domain = recurring.toDomain()

        assertNull(domain.confidenceScore)
        assertNull(domain.medianAmount)
        assertNull(domain.amountVariation)
        assertNull(domain.medianIntervalDays)
        assertNull(domain.intervalVariation)
    }

    @Test
    fun `empty insights, anomalies and recurring lists map to empty domain lists`() {
        val response = AnalyticsResponseDto(
            summary = summaryDto,
            insights = emptyList(),
            anomalies = emptyList(),
            recurring = emptyList(),
            metadata = metadataDto,
        )

        val result = response.toDomain()

        assertTrue(result.insights.isEmpty())
        assertTrue(result.anomalies.isEmpty())
        assertTrue(result.recurring.isEmpty())
    }
}
