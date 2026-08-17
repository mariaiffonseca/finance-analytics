package com.mariafonseca.financeanalytics.core.analytics

import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsResult
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction

/**
 * Data boundary for the Analytics API (PR-014 §6): prepares the request,
 * calls the API, and maps both success and expected failures. Callers never
 * see DTOs, HTTP status codes, or exception types directly.
 */
interface AnalyticsRepository {
    suspend fun analyse(transactions: List<Transaction>): Result<AnalyticsResult>
}
