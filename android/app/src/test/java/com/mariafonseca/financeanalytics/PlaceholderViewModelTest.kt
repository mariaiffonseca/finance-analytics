package com.mariafonseca.financeanalytics

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceholderViewModelTest {

    // Verifies the ViewModel wires the expected resource id. A plain JUnit test has
    // no Context to resolve strings.xml content -- that coverage lives in the
    // instrumented PlaceholderScreenTest, which asserts the resolved text on screen.
    @Test
    fun `initial ui state wires the expected placeholder message resource id`() {
        val viewModel = PlaceholderViewModel()

        val state = viewModel.uiState.value

        assertEquals(R.string.placeholder_message, state.messageRes)
    }
}
