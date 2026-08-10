package com.mariafonseca.financeanalytics.features.workspace.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceViewModelTest {

    @Test
    fun `initial ui state exposes the onboarding headline`() {
        val viewModel = WorkspaceViewModel()

        val state = viewModel.uiState.value

        assertEquals("Understand where your money goes.", state.emptyStateHeadline)
    }

    @Test
    fun `initial ui state exposes exactly three privacy points`() {
        val viewModel = WorkspaceViewModel()

        val state = viewModel.uiState.value

        assertEquals(3, state.privacyPoints.size)
        assertTrue(state.privacyPoints.all { it.isNotBlank() })
    }
}
