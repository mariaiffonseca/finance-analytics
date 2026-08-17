package com.mariafonseca.financeanalytics.features.insights.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mariafonseca.financeanalytics.core.analytics.AnalyticsRepository
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsApiException
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsFailureReason
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsResult
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsUiState
import com.mariafonseca.financeanalytics.features.transactions.data.TransactionRepository
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InsightsViewModel(
    private val transactionRepository: TransactionRepository,
    private val analyticsRepository: AnalyticsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    // Snapshot used by onRetryAnalysis(); local data stays usable
    // independently of analytics (PR-014 §8), so this is only ever read,
    // never required for the rest of the screen to function.
    private var latestTransactions: List<Transaction> = emptyList()

    init {
        viewModelScope.launch {
            transactionRepository.observeTransactions().collect { transactions ->
                latestTransactions = transactions
                // Explicit, one-shot trigger (PR-014 §9): only the first
                // time transactions are seen, not on every subsequent
                // database emission. AppDataState can flip to
                // DataAvailable with zero real transactions (the import
                // flow's "Continue without importing" skip path), so the
                // real local transaction list — not that flag — is the
                // gate for whether there's anything to analyse.
                if (transactions.isNotEmpty() && _uiState.value.analyticsState is AnalyticsUiState.Idle) {
                    runAnalysis(transactions)
                }
            }
        }
    }

    fun onFilterSelected(filter: InsightFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun onRetryAnalysis() {
        val transactions = latestTransactions
        if (transactions.isNotEmpty()) {
            viewModelScope.launch { runAnalysis(transactions) }
        }
    }

    private suspend fun runAnalysis(transactions: List<Transaction>) {
        _uiState.update { it.copy(analyticsState = AnalyticsUiState.Loading) }
        val result = analyticsRepository.analyse(transactions)
        _uiState.update { it.copy(analyticsState = result.toAnalyticsUiState()) }
    }
}

private fun Result<AnalyticsResult>.toAnalyticsUiState(): AnalyticsUiState {
    getOrNull()?.let { return AnalyticsUiState.Success(it) }
    val reason = (exceptionOrNull() as? AnalyticsApiException)?.reason ?: AnalyticsFailureReason.UNEXPECTED
    return when (reason) {
        AnalyticsFailureReason.NO_CONNECTION,
        AnalyticsFailureReason.TIMEOUT,
        AnalyticsFailureReason.SERVER_ERROR,
        -> AnalyticsUiState.Unavailable

        AnalyticsFailureReason.CLIENT_ERROR,
        AnalyticsFailureReason.INVALID_RESPONSE,
        AnalyticsFailureReason.UNEXPECTED,
        -> AnalyticsUiState.Error
    }
}
