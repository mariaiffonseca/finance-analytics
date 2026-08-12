package com.mariafonseca.financeanalytics.features.transactions.presentation

import com.mariafonseca.financeanalytics.R
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionsViewModelTest {

    @Test
    fun `initial ui state exposes the transactions title`() {
        val viewModel = TransactionsViewModel()

        val state = viewModel.uiState.value

        assertEquals(R.string.tab_transactions, state.titleRes)
    }

    @Test
    fun `initial ui state exposes the transactions placeholder message`() {
        val viewModel = TransactionsViewModel()

        val state = viewModel.uiState.value

        assertEquals(R.string.transactions_placeholder_message, state.placeholderMessageRes)
    }
}
