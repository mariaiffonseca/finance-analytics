package com.mariafonseca.financeanalytics.core.common

import com.mariafonseca.financeanalytics.core.testing.FakeTransactionRepository
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AppDataViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `app data state starts as NoData when there is no local data`() = runTest(dispatcher) {
        val viewModel = AppDataViewModel(FakeTransactionRepository())

        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.appDataState.value is AppDataState.NoData)
    }

    @Test
    fun `completing import transitions app data state to DataAvailable`() = runTest(dispatcher) {
        val viewModel = AppDataViewModel(FakeTransactionRepository())

        viewModel.onImportCompleted()

        assertTrue(viewModel.appDataState.value is AppDataState.DataAvailable)
    }

    @Test
    fun `already-persisted transactions surface as DataAvailable on startup`() = runTest(dispatcher) {
        val transaction = Transaction(
            id = 1,
            date = LocalDate.of(2026, 8, 1),
            merchant = "Coffee Shop",
            amount = Money(-450),
            category = "Food",
        )
        val viewModel = AppDataViewModel(FakeTransactionRepository(initialTransactions = listOf(transaction)))

        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.appDataState.value is AppDataState.DataAvailable)
    }
}
