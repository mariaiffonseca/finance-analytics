package com.mariafonseca.financeanalytics.features.transactions.presentation

import androidx.annotation.StringRes
import com.mariafonseca.financeanalytics.R
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction

data class TransactionsUiState(
    @param:StringRes val titleRes: Int = R.string.tab_transactions,
    @param:StringRes val placeholderMessageRes: Int = R.string.transactions_placeholder_message,
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val selectedTransaction: Transaction? = null,
) {
    /** Structured filtering (search + category), never on rendered text — same convention as InsightFilter. */
    val filteredTransactions: List<Transaction>
        get() = transactions.filteredBy(searchQuery, selectedCategory)
}
