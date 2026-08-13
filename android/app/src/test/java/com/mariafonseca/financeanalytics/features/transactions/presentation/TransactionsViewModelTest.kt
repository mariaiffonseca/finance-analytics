package com.mariafonseca.financeanalytics.features.transactions.presentation

import com.mariafonseca.financeanalytics.R
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
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

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
    fun `initial ui state exposes the transactions title`() {
        val viewModel = TransactionsViewModel(FakeTransactionRepository())

        val state = viewModel.uiState.value

        assertEquals(R.string.tab_transactions, state.titleRes)
    }

    @Test
    fun `initial ui state exposes the transactions placeholder message`() {
        val viewModel = TransactionsViewModel(FakeTransactionRepository())

        val state = viewModel.uiState.value

        assertEquals(R.string.transactions_placeholder_message, state.placeholderMessageRes)
    }

    @Test
    fun `ui state reflects transactions emitted by the repository`() = runTest {
        val transaction = Transaction(
            id = 1,
            date = LocalDate.of(2026, 8, 1),
            merchant = "Coffee Shop",
            amount = Money(-450),
            category = "Food",
        )
        val repository = FakeTransactionRepository(initialTransactions = listOf(transaction))

        val viewModel = TransactionsViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(transaction), viewModel.uiState.value.transactions)
    }
}
