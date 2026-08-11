package com.mariafonseca.financeanalytics.features.insights.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InsightsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    fun onFilterSelected(filter: InsightFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }
}
