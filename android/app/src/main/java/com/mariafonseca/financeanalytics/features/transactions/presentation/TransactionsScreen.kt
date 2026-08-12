package com.mariafonseca.financeanalytics.features.transactions.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mariafonseca.financeanalytics.core.designsystem.component.PlaceholderScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PlaceholderScreen(
        titleRes = uiState.titleRes,
        messageRes = uiState.placeholderMessageRes,
    )
}
