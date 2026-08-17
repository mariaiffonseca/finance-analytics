package com.mariafonseca.financeanalytics.core.analytics

import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsRequestDto
import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsResponseDto
import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsMetadataDto
import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsSummaryDto
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsApiException
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsFailureReason
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class AnalyticsApiClientImplTest {

    private val request = AnalyticsRequestDto(transactions = emptyList())
    private val response = AnalyticsResponseDto(
        summary = AnalyticsSummaryDto(
            transactionCount = 0,
            incomeCount = 0,
            expenseCount = 0,
            totalIncome = 0.0,
            totalExpenses = 0.0,
            netSavings = 0.0,
            dateRangeStart = null,
            dateRangeEnd = null,
            uniqueMerchantCount = 0,
            uniqueCategoryCount = 0,
        ),
        insights = emptyList(),
        anomalies = emptyList(),
        recurring = emptyList(),
        metadata = AnalyticsMetadataDto(0, 0, 0, 0, 0, 0, 0, 0, 0),
    )

    @Test
    fun `a successful response is returned as-is`() = runTest {
        val client = AnalyticsApiClientImpl(FakeAnalyticsApi(result = { response }))

        assertSame(response, client.analyse(request))
    }

    @Test
    fun `a 4xx http error is categorized as a client error`() = runTest {
        val client = AnalyticsApiClientImpl(FakeAnalyticsApi(result = { throw httpException(422) }))

        val exception = assertThrowsAnalyticsApiException { client.analyse(request) }

        assertEquals(AnalyticsFailureReason.CLIENT_ERROR, exception.reason)
    }

    @Test
    fun `a 5xx http error is categorized as a server error`() = runTest {
        val client = AnalyticsApiClientImpl(FakeAnalyticsApi(result = { throw httpException(500) }))

        val exception = assertThrowsAnalyticsApiException { client.analyse(request) }

        assertEquals(AnalyticsFailureReason.SERVER_ERROR, exception.reason)
    }

    @Test
    fun `a malformed response is categorized as an invalid response`() = runTest {
        val client = AnalyticsApiClientImpl(
            FakeAnalyticsApi(result = { throw SerializationException("unexpected JSON token") }),
        )

        val exception = assertThrowsAnalyticsApiException { client.analyse(request) }

        assertEquals(AnalyticsFailureReason.INVALID_RESPONSE, exception.reason)
    }

    @Test
    fun `a socket timeout is categorized as a timeout`() = runTest {
        val client = AnalyticsApiClientImpl(FakeAnalyticsApi(result = { throw SocketTimeoutException() }))

        val exception = assertThrowsAnalyticsApiException { client.analyse(request) }

        assertEquals(AnalyticsFailureReason.TIMEOUT, exception.reason)
    }

    @Test
    fun `a network exception is categorized as no connection`() = runTest {
        val client = AnalyticsApiClientImpl(FakeAnalyticsApi(result = { throw IOException("no route to host") }))

        val exception = assertThrowsAnalyticsApiException { client.analyse(request) }

        assertEquals(AnalyticsFailureReason.NO_CONNECTION, exception.reason)
    }

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<AnalyticsResponseDto>(code, "".toResponseBody(null)))

    private suspend fun assertThrowsAnalyticsApiException(block: suspend () -> Unit): AnalyticsApiException {
        try {
            block()
        } catch (e: AnalyticsApiException) {
            return e
        }
        throw AssertionError("Expected an AnalyticsApiException to be thrown")
    }
}

private class FakeAnalyticsApi(private val result: () -> AnalyticsResponseDto) : AnalyticsApi {
    override suspend fun analyse(request: AnalyticsRequestDto): AnalyticsResponseDto = result()
}
