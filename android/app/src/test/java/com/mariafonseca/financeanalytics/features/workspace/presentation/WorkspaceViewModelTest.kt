package com.mariafonseca.financeanalytics.features.workspace.presentation

import com.mariafonseca.financeanalytics.R
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceViewModelTest {

    @Test
    fun `initial ui state exposes the onboarding headline`() {
        val viewModel = WorkspaceViewModel()

        val state = viewModel.uiState.value

        assertEquals(R.string.empty_state_headline, state.emptyStateHeadlineRes)
    }

    @Test
    fun `initial ui state exposes exactly three privacy points`() {
        val viewModel = WorkspaceViewModel()

        val state = viewModel.uiState.value

        assertEquals(3, state.privacyPointsRes.size)
    }
}
