package com.mariafonseca.financeanalytics.features.insights.presentation

import com.mariafonseca.financeanalytics.core.analytics.AnalyticsRepository
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsApiException
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsFailureReason
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsResult
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsSummary
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsUiState
import com.mariafonseca.financeanalytics.core.common.Money
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val transaction = Transaction(
        id = 1,
        date = LocalDate.of(2026, 8, 1),
        merchant = "Coffee Shop",
        amount = Money(-450),
        category = "Food",
    )

    private val successResult = AnalyticsResult(
        summary = AnalyticsSummary(
            transactionCount = 1,
            incomeCount = 0,
            expenseCount = 1,
            totalIncome = 0.0,
            totalExpenses = -4.5,
            netSavings = -4.5,
            dateRangeStart = null,
            dateRangeEnd = null,
            uniqueMerchantCount = 1,
            uniqueCategoryCount = 1,
        ),
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
    fun `initial ui state selects the Recent filter`() {
        val viewModel = buildViewModel()

        assertEquals(InsightFilter.RECENT, viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun `initial ui state exposes all five filters`() {
        val viewModel = buildViewModel()

        assertEquals(
            listOf(
                InsightFilter.RECENT,
                InsightFilter.SPENDING,
                InsightFilter.TRENDS,
                InsightFilter.ANOMALIES,
                InsightFilter.RECURRING,
            ),
            viewModel.uiState.value.filters,
        )
    }

    @Test
    fun `selecting a filter updates the selected filter`() {
        val viewModel = buildViewModel()

        viewModel.onFilterSelected(InsightFilter.ANOMALIES)

        assertEquals(InsightFilter.ANOMALIES, viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun `analytics state stays idle when there are no local transactions`() = runTest(dispatcher) {
        val viewModel = buildViewModel(transactionRepository = FakeTransactionRepository())

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AnalyticsUiState.Idle, viewModel.uiState.value.analyticsState)
    }

    @Test
    fun `analytics succeeds once local transactions are available`() = runTest(dispatcher) {
        val viewModel = buildViewModel(
            transactionRepository = FakeTransactionRepository(initialTransactions = listOf(transaction)),
            analyticsRepository = FakeAnalyticsRepository(result = { Result.success(successResult) }),
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AnalyticsUiState.Success(successResult),
            viewModel.uiState.value.analyticsState,
        )
    }

    @Test
    fun `a connectivity failure surfaces as unavailable`() = runTest(dispatcher) {
        val viewModel = buildViewModel(
            transactionRepository = FakeTransactionRepository(initialTransactions = listOf(transaction)),
            analyticsRepository = FakeAnalyticsRepository(
                result = {
                    Result.failure(AnalyticsApiException(AnalyticsFailureReason.NO_CONNECTION, "unreachable"))
                },
            ),
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AnalyticsUiState.Unavailable, viewModel.uiState.value.analyticsState)
    }

    @Test
    fun `a request rejection surfaces as an error`() = runTest(dispatcher) {
        val viewModel = buildViewModel(
            transactionRepository = FakeTransactionRepository(initialTransactions = listOf(transaction)),
            analyticsRepository = FakeAnalyticsRepository(
                result = {
                    Result.failure(AnalyticsApiException(AnalyticsFailureReason.CLIENT_ERROR, "rejected"))
                },
            ),
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AnalyticsUiState.Error, viewModel.uiState.value.analyticsState)
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

    @Test
    fun `a later transaction change does not automatically re-trigger analysis`() = runTest(dispatcher) {
        val transactionRepository = FakeTransactionRepository(initialTransactions = listOf(transaction))
        val analyticsRepository = FakeAnalyticsRepository(result = { Result.success(successResult) })
        buildViewModel(transactionRepository = transactionRepository, analyticsRepository = analyticsRepository)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, analyticsRepository.callCount)

        transactionRepository.insertTransactions(listOf(transaction.copy(id = 2, merchant = "Grocer")))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, analyticsRepository.callCount)
    }

    private fun buildViewModel(
        transactionRepository: FakeTransactionRepository = FakeTransactionRepository(),
        analyticsRepository: AnalyticsRepository = FakeAnalyticsRepository(result = { Result.success(successResult) }),
    ): InsightsViewModel = InsightsViewModel(transactionRepository, analyticsRepository)
}

private class FakeAnalyticsRepository(
    var result: () -> Result<AnalyticsResult>,
) : AnalyticsRepository {
    var callCount = 0
        private set

    override suspend fun analyse(transactions: List<Transaction>): Result<AnalyticsResult> {
        callCount++
        return result()
    }
}
