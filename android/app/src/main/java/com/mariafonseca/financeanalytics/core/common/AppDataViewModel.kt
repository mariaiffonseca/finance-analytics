package com.mariafonseca.financeanalytics.core.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mariafonseca.financeanalytics.features.transactions.data.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Owns [AppDataState], the app-wide phase split between the import flow and
 * the analytics workspace. Hosted once at the navigation root (see
 * FinanceAnalyticsNavHost) so any screen that needs it — now Import's
 * Continue action, later e.g. Settings' reset — depends on this directly
 * instead of reaching into a feature screen's own ViewModel.
 */
class AppDataViewModel(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _appDataState = MutableStateFlow<AppDataState>(AppDataState.NoData)
    val appDataState: StateFlow<AppDataState> = _appDataState.asStateFlow()

    init {
        // Room persists across process restarts even though this ViewModel
        // (and therefore in-memory AppDataState) does not: without this
        // check, a user who already imported data would see the Empty/
        // Import welcome flow again every time the app process is killed
        // and relaunched, even though their data is still on the device.
        viewModelScope.launch {
            if (transactionRepository.observeTransactions().first().isNotEmpty()) {
                _appDataState.value = AppDataState.DataAvailable
            }
        }
    }

    // Called once the import flow's Continue action fires.
    fun onImportCompleted() {
        _appDataState.value = AppDataState.DataAvailable
    }
}
