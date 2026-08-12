package com.mariafonseca.financeanalytics.features.insights.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsViewModelTest {

    @Test
    fun `initial ui state selects the Recent filter`() {
        val viewModel = InsightsViewModel()

        val state = viewModel.uiState.value

        assertEquals(InsightFilter.RECENT, state.selectedFilter)
    }

    @Test
    fun `initial ui state exposes all five filters`() {
        val viewModel = InsightsViewModel()

        val state = viewModel.uiState.value

        assertEquals(
            listOf(
                InsightFilter.RECENT,
                InsightFilter.SPENDING,
                InsightFilter.TRENDS,
                InsightFilter.ANOMALIES,
                InsightFilter.RECURRING,
            ),
            state.filters,
        )
    }

    @Test
    fun `selecting a filter updates the selected filter`() {
        val viewModel = InsightsViewModel()

        viewModel.onFilterSelected(InsightFilter.ANOMALIES)

        assertEquals(InsightFilter.ANOMALIES, viewModel.uiState.value.selectedFilter)
    }
}
