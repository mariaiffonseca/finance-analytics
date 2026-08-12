package com.mariafonseca.financeanalytics.features.overview.presentation

import com.mariafonseca.financeanalytics.R
import org.junit.Assert.assertEquals
import org.junit.Test

class OverviewViewModelTest {

    @Test
    fun `initial ui state exposes the overview title`() {
        val viewModel = OverviewViewModel()

        val state = viewModel.uiState.value

        assertEquals(R.string.tab_overview, state.titleRes)
    }

    @Test
    fun `initial ui state exposes the overview placeholder message`() {
        val viewModel = OverviewViewModel()

        val state = viewModel.uiState.value

        assertEquals(R.string.overview_placeholder_message, state.placeholderMessageRes)
    }
}
