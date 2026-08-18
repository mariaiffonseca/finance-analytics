package com.mariafonseca.financeanalytics.features.overview.presentation

import com.mariafonseca.financeanalytics.R
import com.mariafonseca.financeanalytics.core.analytics.AnalyticsRepository
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsApiException
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsFailureReason
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsResult
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsSummary
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsUiState
import com.mariafonseca.financeanalytics.core.common.Money
import com.mariafonseca.financeanalytics.core.testing.FakeAnalyticsRepository
import com.mariafonseca.financeanalytics.core.testing.FakeTransactionRepository
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val transaction = Transaction(
        id = 1,
        date = LocalDate.of(2026, 8, 1),
        merchant = "Coffee Shop",
        amount = Money(-450),
        category = "Food",
    )

    private val successResult = AnalyticsResult(
        summary = AnalyticsSummary(1, 0, 1, 0.0, -4.5, -4.5, null, null, 1, 1),
        insights = emptyList(),
        anomalies = emptyList(),
        recurring = emptyList(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial ui state exposes the overview title`() {
        val viewModel = buildViewModel()

        assertEquals(R.string.tab_overview, viewModel.uiState.value.titleRes)
    }

    @Test
    fun `initial ui state has no monthly overview`() {
        val viewModel = buildViewModel()

        assertNull(viewModel.uiState.value.monthlyOverview)
    }

    @Test
    fun `ui state exposes a monthly overview once local transactions are available`() = runTest(dispatcher) {
        val viewModel = buildViewModel(
            transactionRepository = FakeTransactionRepository(initialTransactions = listOf(transaction)),
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("August 2026", viewModel.uiState.value.monthlyOverview?.monthLabel)
    }

    @Test
    fun `analytics succeeds once local transactions are available`() = runTest(dispatcher) {
        val viewModel = buildViewModel(
            transactionRepository = FakeTransactionRepository(initialTransactions = listOf(transaction)),
            analyticsRepository = FakeAnalyticsRepository(result = { Result.success(successResult) }),
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AnalyticsUiState.Success(successResult), viewModel.uiState.value.analyticsState)
    }

    @Test
    fun `a connectivity failure surfaces as unavailable without losing the local overview`() = runTest(dispatcher) {
        val viewModel = buildViewModel(
            transactionRepository = FakeTransactionRepository(initialTransactions = listOf(transaction)),
            analyticsRepository = FakeAnalyticsRepository(
                result = { Result.failure(AnalyticsApiException(AnalyticsFailureReason.NO_CONNECTION, "unreachable")) },
            ),
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AnalyticsUiState.Unavailable, viewModel.uiState.value.analyticsState)
        assertEquals("August 2026", viewModel.uiState.value.monthlyOverview?.monthLabel)
    }

    @Test
    fun `retry re-runs analysis after a failure`() = runTest(dispatcher) {
        val analyticsRepository = FakeAnalyticsRepository(
            result = { Result.failure(AnalyticsApiException(AnalyticsFailureReason.NO_CONNECTION, "unreachable")) },
        )
        val viewModel = buildViewModel(
            transactionRepository = FakeTransactionRepository(initialTransactions = listOf(transaction)),
            analyticsRepository = analyticsRepository,
        )
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(AnalyticsUiState.Unavailable, viewModel.uiState.value.analyticsState)
        analyticsRepository.result = { Result.success(successResult) }

        viewModel.onRetryAnalysis()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AnalyticsUiState.Success(successResult), viewModel.uiState.value.analyticsState)
        assertEquals(2, analyticsRepository.callCount)
    }

    private fun buildViewModel(
        transactionRepository: FakeTransactionRepository = FakeTransactionRepository(),
        analyticsRepository: AnalyticsRepository = FakeAnalyticsRepository(result = { Result.success(successResult) }),
    ): OverviewViewModel = OverviewViewModel(transactionRepository, analyticsRepository)
}
