package com.mariafonseca.financeanalytics.core.common

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns [AppDataState], the app-wide phase split between the import flow and
 * the analytics workspace. Hosted once at the navigation root (see
 * FinanceAnalyticsNavHost) so any screen that needs it — now Import's
 * Continue action, later e.g. Settings' reset — depends on this directly
 * instead of reaching into a feature screen's own ViewModel.
 */
class AppDataViewModel : ViewModel() {

    private val _appDataState = MutableStateFlow<AppDataState>(AppDataState.NoData)
    val appDataState: StateFlow<AppDataState> = _appDataState.asStateFlow()

    // Called once the import flow's Continue action fires. Today that action
    // has no real data behind it — this only models the conceptual
    // NoData -> DataAvailable transition until the CSV pipeline lands.
    fun onImportCompleted() {
        _appDataState.value = AppDataState.DataAvailable
    }
}
