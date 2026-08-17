package com.mariafonseca.financeanalytics.core.analytics

import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsRequestDto
import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

/** Retrofit declaration of `POST /analytics/analyse` (PR-013 §4). Transport only — see [AnalyticsApiClient] for failure handling. */
interface AnalyticsApi {
    @POST("analytics/analyse")
    suspend fun analyse(@Body request: AnalyticsRequestDto): AnalyticsResponseDto
}
