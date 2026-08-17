package com.mariafonseca.financeanalytics.core.analytics.model

import java.time.LocalDate

/** Domain result of one `POST /analytics/analyse` call, mapped from AnalyticsResponseDto. */
data class AnalyticsResult(
    val summary: AnalyticsSummary,
    val insights: List<Insight>,
    val anomalies: List<Anomaly>,
    val recurring: List<RecurringTransaction>,
)

data class AnalyticsSummary(
    val transactionCount: Int,
    val incomeCount: Int,
    val expenseCount: Int,
    val totalIncome: Double,
    val totalExpenses: Double,
    val netSavings: Double,
    val dateRangeStart: LocalDate?,
    val dateRangeEnd: LocalDate?,
    val uniqueMerchantCount: Int,
    val uniqueCategoryCount: Int,
)

/**
 * Mirrors `InsightResponse`'s actual fields from the running PR-013 API
 * (analytics/src/finance_analytics/api/schemas.py), not
 * docs/project/02_DOMAIN_MODEL.md's aspirational `Insight` entity — that
 * doc's `priority`/`generatedAt` fields are not part of the real contract,
 * and the API contract is the source of truth (PR-014's Critical Rule).
 * `metadata` is intentionally not carried over from InsightDto: it's an
 * open, untyped bag, not meant for direct UI display (PR-014 §11).
 */
data class Insight(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    val severity: String,
    val confidence: Double,
    val relatedTransactionIds: List<String>,
    val category: String?,
    val merchant: String?,
    val amount: Double?,
    val comparisonPeriod: String?,
)

/** `reference_context` is dropped for the same reason as [Insight.metadata] above. */
data class Anomaly(
    val transactionId: String,
    val anomalyScore: Double?,
    val isAnomaly: Boolean,
    val method: String,
    val reason: String,
)

data class RecurringTransaction(
    val merchant: String,
    val currency: String,
    val isRecurring: Boolean,
    val classification: String,
    val confidenceScore: Double?,
    val frequency: String,
    val occurrences: Int,
    val medianAmount: Double?,
    val amountVariation: Double?,
    val medianIntervalDays: Double?,
    val intervalVariation: Double?,
    val firstSeen: LocalDate,
    val lastSeen: LocalDate,
    val reason: String,
    val transactionIds: List<String>,
)
