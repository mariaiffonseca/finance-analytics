package com.mariafonseca.financeanalytics

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceholderViewModelTest {

    @Test
    fun `initial ui state exposes the foundation placeholder message resource`() {
        val viewModel = PlaceholderViewModel()

        val state = viewModel.uiState.value

        assertEquals(R.string.placeholder_message, state.messageRes)
    }
}
