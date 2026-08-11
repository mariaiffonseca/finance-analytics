package com.mariafonseca.financeanalytics.features.workspace.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mariafonseca.financeanalytics.R

@Composable
private fun StubTabScreen(@StringRes titleRes: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.stub_screen_coming_soon, stringResource(titleRes)),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
fun OverviewStubScreen() {
    StubTabScreen(titleRes = R.string.tab_overview)
}

@Composable
fun InsightsStubScreen() {
    StubTabScreen(titleRes = R.string.tab_insights)
}

@Composable
fun TransactionsStubScreen() {
    StubTabScreen(titleRes = R.string.tab_transactions)
}
