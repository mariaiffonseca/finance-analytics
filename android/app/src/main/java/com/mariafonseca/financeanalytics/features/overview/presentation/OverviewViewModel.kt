package com.mariafonseca.financeanalytics.features.overview.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mariafonseca.financeanalytics.core.analytics.AnalyticsRepository
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsUiState
import com.mariafonseca.financeanalytics.core.analytics.model.toAnalyticsUiState
import com.mariafonseca.financeanalytics.features.transactions.data.TransactionRepository
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The dashboard's headline numbers (total spent, income, saved, category
 * breakdown, spending trend) come from local transactions and update on
 * every database emission — see [MonthlyOverview]. The Top/More Insights
 * section needs the Insights Engine, so it follows the same one-shot
 * analytics trigger as [com.mariafonseca.financeanalytics.features.insights.presentation.InsightsViewModel]
 * (PR-014 §9): only the first time transactions are seen, not on every
 * subsequent emission.
 */
class OverviewViewModel(
    private val transactionRepository: TransactionRepository,
    private val analyticsRepository: AnalyticsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    private var latestTransactions: List<Transaction> = emptyList()

    init {
        viewModelScope.launch {
            transactionRepository.observeTransactions().collect { transactions ->
                latestTransactions = transactions
                _uiState.update { it.copy(monthlyOverview = transactions.toMonthlyOverview()) }
                if (transactions.isNotEmpty() && _uiState.value.analyticsState is AnalyticsUiState.Idle) {
                    runAnalysis(transactions)
                }
            }
        }
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
