package com.mariafonseca.financeanalytics

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaceholderViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        PlaceholderUiState(messageRes = R.string.placeholder_message),
    )
    val uiState: StateFlow<PlaceholderUiState> = _uiState.asStateFlow()
}
