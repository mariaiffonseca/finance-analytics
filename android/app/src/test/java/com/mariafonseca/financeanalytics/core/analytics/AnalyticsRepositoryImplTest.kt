package com.mariafonseca.financeanalytics.core.analytics

import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsMetadataDto
import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsRequestDto
import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsResponseDto
import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsSummaryDto
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsApiException
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsFailureReason
import com.mariafonseca.financeanalytics.core.common.Money
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AnalyticsRepositoryImplTest {

    private val transaction = Transaction(
        id = 1,
        date = LocalDate.of(2026, 8, 1),
        merchant = "Coffee Shop",
        amount = Money(-450),
        category = "Food",
    )

    private val responseDto = AnalyticsResponseDto(
        summary = AnalyticsSummaryDto(
            transactionCount = 1,
            incomeCount = 0,
            expenseCount = 1,
            totalIncome = 0.0,
            totalExpenses = -4.5,
            netSavings = -4.5,
            dateRangeStart = "2026-08-01",
            dateRangeEnd = "2026-08-01",
            uniqueMerchantCount = 1,
            uniqueCategoryCount = 1,
        ),
        insights = emptyList(),
        anomalies = emptyList(),
        recurring = emptyList(),
        metadata = AnalyticsMetadataDto(1, 1, 0, 0, 0, 0, 0, 0, 0),
    )

    @Test
    fun `a successful analysis maps the response into a domain result`() = runTest {
        val repository = AnalyticsRepositoryImpl(FakeAnalyticsApiClient(result = { responseDto }))

        val result = repository.analyse(listOf(transaction))

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().summary.transactionCount)
    }

    @Test
    fun `an api failure is returned as a failed result carrying the failure reason`() = runTest {
        val repository = AnalyticsRepositoryImpl(
            FakeAnalyticsApiClient(
                result = {
                    throw AnalyticsApiException(AnalyticsFailureReason.CLIENT_ERROR, "Analytics API returned HTTP 422.")
                },
            ),
        )

        val result = repository.analyse(listOf(transaction))

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as AnalyticsApiException
        assertEquals(AnalyticsFailureReason.CLIENT_ERROR, exception.reason)
    }

    @Test
    fun `a network failure is returned as a failed result`() = runTest {
        val repository = AnalyticsRepositoryImpl(
            FakeAnalyticsApiClient(
                result = {
                    throw AnalyticsApiException(AnalyticsFailureReason.NO_CONNECTION, "Could not reach the analytics API.")
                },
            ),
        )

        val result = repository.analyse(listOf(transaction))

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as AnalyticsApiException
        assertEquals(AnalyticsFailureReason.NO_CONNECTION, exception.reason)
    }

    @Test
    fun `an unexpected exception from the api client is wrapped as an unexpected failure`() = runTest {
        val repository = AnalyticsRepositoryImpl(
            FakeAnalyticsApiClient(result = { throw IllegalStateException("boom") }),
        )

        val result = repository.analyse(listOf(transaction))

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as AnalyticsApiException
        assertEquals(AnalyticsFailureReason.UNEXPECTED, exception.reason)
    }
}

private class FakeAnalyticsApiClient(private val result: () -> AnalyticsResponseDto) : AnalyticsApiClient {
    override suspend fun analyse(request: AnalyticsRequestDto): AnalyticsResponseDto = result()
}
