package com.mariafonseca.financeanalytics.features.transactions.presentation

import androidx.annotation.StringRes
import com.mariafonseca.financeanalytics.R

data class TransactionsUiState(
    @param:StringRes val titleRes: Int = R.string.tab_transactions,
    @param:StringRes val placeholderMessageRes: Int = R.string.transactions_placeholder_message,
)
