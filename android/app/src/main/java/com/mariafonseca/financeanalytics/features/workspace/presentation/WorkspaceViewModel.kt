package com.mariafonseca.financeanalytics.features.workspace.presentation

import androidx.lifecycle.ViewModel
import com.mariafonseca.financeanalytics.core.common.AppDataState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WorkspaceViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

    private val _appDataState = MutableStateFlow<AppDataState>(AppDataState.NoData)
    val appDataState: StateFlow<AppDataState> = _appDataState.asStateFlow()

    // Called once the import flow's Continue action fires. Today that action
    // has no real data behind it — this only models the conceptual
    // NoData -> DataAvailable transition until the CSV pipeline lands.
    fun onImportCompleted() {
        _appDataState.value = AppDataState.DataAvailable
    }
}
