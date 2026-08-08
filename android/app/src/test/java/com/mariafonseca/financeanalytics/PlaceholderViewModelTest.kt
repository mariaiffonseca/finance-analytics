package com.mariafonseca.financeanalytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceholderViewModelTest {

    @Test
    fun `initial ui state exposes a non-empty message`() {
        val viewModel = PlaceholderViewModel()

        val state = viewModel.uiState.value

        assertTrue(state.message.isNotBlank())
    }

    @Test
    fun `initial ui state matches the expected foundation message`() {
        val viewModel = PlaceholderViewModel()

        val state = viewModel.uiState.value

        assertEquals("Finance Analytics — foundation build", state.message)
    }
}
