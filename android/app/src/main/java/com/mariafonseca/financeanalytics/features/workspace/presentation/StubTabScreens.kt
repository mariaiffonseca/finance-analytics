package com.mariafonseca.financeanalytics.features.workspace.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
private fun StubTabScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$title — coming soon", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun OverviewStubScreen() {
    StubTabScreen(title = "Overview")
}

@Composable
fun InsightsStubScreen() {
    StubTabScreen(title = "Insights")
}

@Composable
fun TransactionsStubScreen() {
    StubTabScreen(title = "Transactions")
}
