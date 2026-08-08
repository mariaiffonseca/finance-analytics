package com.mariafonseca.financeanalytics

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaceholderViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        PlaceholderUiState(message = "Finance Analytics — foundation build"),
    )
    val uiState: StateFlow<PlaceholderUiState> = _uiState.asStateFlow()
}
