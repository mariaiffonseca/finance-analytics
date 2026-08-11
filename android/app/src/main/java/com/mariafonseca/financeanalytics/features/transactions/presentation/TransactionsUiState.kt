package com.mariafonseca.financeanalytics.features.transactions.presentation

import androidx.annotation.StringRes
import com.mariafonseca.financeanalytics.R

data class TransactionsUiState(
    @param:StringRes val titleRes: Int = R.string.transactions_title,
    @param:StringRes val placeholderMessageRes: Int = R.string.transactions_placeholder_message,
)
