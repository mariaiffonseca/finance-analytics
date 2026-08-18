package com.mariafonseca.financeanalytics.features.transactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mariafonseca.financeanalytics.features.transactions.data.TransactionRepository
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            transactionRepository.observeTransactions().collect { transactions ->
                _uiState.update {
                    it.copy(
                        transactions = transactions,
                        categories = transactions.distinctCategories(),
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelected(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onTransactionSelected(transaction: Transaction) {
        _uiState.update { it.copy(selectedTransaction = transaction) }
    }

    fun onTransactionDetailDismissed() {
        _uiState.update { it.copy(selectedTransaction = null) }
    }
}

private fun List<Transaction>.distinctCategories(): List<String> =
    mapNotNull { it.category?.takeIf(String::isNotBlank) }.distinct().sorted()
