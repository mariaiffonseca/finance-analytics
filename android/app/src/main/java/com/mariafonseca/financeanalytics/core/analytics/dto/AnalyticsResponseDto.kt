package com.mariafonseca.financeanalytics.core.analytics.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Mirrors `AnalysisResponse` and its nested schemas in
 * analytics/src/finance_analytics/api/schemas.py (PR-013 §6) field for
 * field — verified against the app's own generated `openapi.json`, the
 * source of truth per PR-014's Critical Rule. Do not add or rename fields
 * here without re-checking that contract.
 */
@Serializable
data class AnalyticsResponseDto(
    val summary: AnalyticsSummaryDto,
    val insights: List<InsightDto>,
    val anomalies: List<AnomalyDto>,
    val recurring: List<RecurringDto>,
    val metadata: AnalyticsMetadataDto,
)

@Serializable
data class AnalyticsSummaryDto(
    @SerialName("transaction_count") val transactionCount: Int,
    @SerialName("income_count") val incomeCount: Int,
    @SerialName("expense_count") val expenseCount: Int,
    @SerialName("total_income") val totalIncome: Double,
    @SerialName("total_expenses") val totalExpenses: Double,
    @SerialName("net_savings") val netSavings: Double,
    @SerialName("date_range_start") val dateRangeStart: String? = null,
    @SerialName("date_range_end") val dateRangeEnd: String? = null,
    @SerialName("unique_merchant_count") val uniqueMerchantCount: Int,
    @SerialName("unique_category_count") val uniqueCategoryCount: Int,
)

/**
 * `metadata` is a JSON object with no fixed schema on the Python side
 * (`dict[str, Any]`) — kept as [JsonObject] rather than a typed field.
 * Deliberately not carried into the domain model (see AnalyticsMapper.kt):
 * PR-014 §11 says not to expose raw JSON/technical metadata to the UI.
 */
@Serializable
data class InsightDto(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    val severity: String,
    val confidence: Double,
    @SerialName("related_transaction_ids") val relatedTransactionIds: List<String>,
    val metadata: JsonObject,
    val category: String? = null,
    val merchant: String? = null,
    val amount: Double? = null,
    @SerialName("comparison_period") val comparisonPeriod: String? = null,
)

/** `reference_context` is dropped in the domain mapping for the same reason as [InsightDto.metadata]. */
@Serializable
data class AnomalyDto(
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("anomaly_score") val anomalyScore: Double? = null,
    @SerialName("is_anomaly") val isAnomaly: Boolean,
    val method: String,
    val reason: String,
    @SerialName("reference_context") val referenceContext: JsonObject,
)

@Serializable
data class RecurringDto(
    val merchant: String,
    val currency: String,
    @SerialName("is_recurring") val isRecurring: Boolean,
    val classification: String,
    @SerialName("confidence_score") val confidenceScore: Double? = null,
    val frequency: String,
    val occurrences: Int,
    @SerialName("median_amount") val medianAmount: Double? = null,
    @SerialName("amount_variation") val amountVariation: Double? = null,
    @SerialName("median_interval_days") val medianIntervalDays: Double? = null,
    @SerialName("interval_variation") val intervalVariation: Double? = null,
    @SerialName("first_seen") val firstSeen: String,
    @SerialName("last_seen") val lastSeen: String,
    val reason: String,
    @SerialName("transaction_ids") val transactionIds: List<String>,
)

/**
 * Diagnostic/run counters, distinct from the financial [AnalyticsSummaryDto].
 * Kept for contract fidelity but not mapped into the domain model — nothing
 * in this PR's UI needs it (see AnalyticsMapper.kt).
 */
@Serializable
data class AnalyticsMetadataDto(
    @SerialName("requested_transaction_count") val requestedTransactionCount: Int,
    @SerialName("processed_transaction_count") val processedTransactionCount: Int,
    @SerialName("duplicate_row_count") val duplicateRowCount: Int,
    @SerialName("duplicate_id_count") val duplicateIdCount: Int,
    @SerialName("invalid_date_count") val invalidDateCount: Int,
    @SerialName("invalid_amount_count") val invalidAmountCount: Int,
    @SerialName("insight_count") val insightCount: Int,
    @SerialName("anomaly_count") val anomalyCount: Int,
    @SerialName("recurring_count") val recurringCount: Int,
)
