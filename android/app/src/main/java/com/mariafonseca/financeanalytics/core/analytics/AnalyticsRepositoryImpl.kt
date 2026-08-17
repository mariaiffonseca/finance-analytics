package com.mariafonseca.financeanalytics.core.analytics

import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsApiException
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsFailureReason
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsResult
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import kotlinx.coroutines.CancellationException

class AnalyticsRepositoryImpl(
    private val apiClient: AnalyticsApiClient,
) : AnalyticsRepository {

    override suspend fun analyse(transactions: List<Transaction>): Result<AnalyticsResult> =
        try {
            val response = apiClient.analyse(transactions.toRequestDto())
            Result.success(response.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: AnalyticsApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            // AnalyticsApiClient only throws AnalyticsApiException; this is
            // a last-resort safety net for anything else (e.g. a mapping
            // bug), so a caller never has to catch a raw exception type.
            Result.failure(AnalyticsApiException(AnalyticsFailureReason.UNEXPECTED, "Unexpected analytics failure.", e))
        }
}
