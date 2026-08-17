package com.mariafonseca.financeanalytics.core.analytics

import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsRequestDto
import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsResponseDto
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsApiException
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsFailureReason
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

class AnalyticsApiClientImpl(private val api: AnalyticsApi) : AnalyticsApiClient {

    override suspend fun analyse(request: AnalyticsRequestDto): AnalyticsResponseDto {
        try {
            return api.analyse(request)
        } catch (e: HttpException) {
            val reason = if (e.code() in 400..499) {
                AnalyticsFailureReason.CLIENT_ERROR
            } else {
                AnalyticsFailureReason.SERVER_ERROR
            }
            throw AnalyticsApiException(reason, "Analytics API returned HTTP ${e.code()}.", e)
        } catch (e: SerializationException) {
            throw AnalyticsApiException(
                AnalyticsFailureReason.INVALID_RESPONSE,
                "Analytics API returned a response that could not be parsed.",
                e,
            )
        } catch (e: SocketTimeoutException) {
            // Must be caught before the broader IOException below: it is a subtype.
            throw AnalyticsApiException(AnalyticsFailureReason.TIMEOUT, "Analytics API request timed out.", e)
        } catch (e: IOException) {
            throw AnalyticsApiException(AnalyticsFailureReason.NO_CONNECTION, "Could not reach the analytics API.", e)
        }
    }
}
