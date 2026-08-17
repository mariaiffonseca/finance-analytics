package com.mariafonseca.financeanalytics.core.analytics

import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsRequestDto
import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsResponseDto

/**
 * Executes the analytics HTTP call and translates network/HTTP failures
 * into a categorized AnalyticsApiException (PR-014 §3). Does not shape
 * requests, map to domain models, or decide UI state — that is
 * AnalyticsRepositoryImpl's job. A separate interface from [AnalyticsApi]
 * (mirroring TransactionRepository/TransactionDao's split) so
 * AnalyticsRepositoryImplTest can fake this layer directly, independent of
 * AnalyticsApiClientImplTest's own HTTP-failure-translation coverage.
 */
interface AnalyticsApiClient {
    suspend fun analyse(request: AnalyticsRequestDto): AnalyticsResponseDto
}
