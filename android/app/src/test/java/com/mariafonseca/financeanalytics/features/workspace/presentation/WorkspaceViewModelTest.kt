package com.mariafonseca.financeanalytics.features.workspace.presentation

import com.mariafonseca.financeanalytics.R
import com.mariafonseca.financeanalytics.core.common.AppDataState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `app data state starts as NoData`() {
        val viewModel = WorkspaceViewModel()

        assertTrue(viewModel.appDataState.value is AppDataState.NoData)
    }

    @Test
    fun `completing import transitions app data state to DataAvailable`() {
        val viewModel = WorkspaceViewModel()

        viewModel.onImportCompleted()

        assertTrue(viewModel.appDataState.value is AppDataState.DataAvailable)
    }
}
