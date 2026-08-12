package com.mariafonseca.financeanalytics.core.common

import org.junit.Assert.assertTrue
import org.junit.Test

class AppDataViewModelTest {

    @Test
    fun `app data state starts as NoData`() {
        val viewModel = AppDataViewModel()

        assertTrue(viewModel.appDataState.value is AppDataState.NoData)
    }

    @Test
    fun `completing import transitions app data state to DataAvailable`() {
        val viewModel = AppDataViewModel()

        viewModel.onImportCompleted()

        assertTrue(viewModel.appDataState.value is AppDataState.DataAvailable)
    }
}
