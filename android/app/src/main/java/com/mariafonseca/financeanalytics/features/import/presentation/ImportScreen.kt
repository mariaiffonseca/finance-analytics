package com.mariafonseca.financeanalytics.features.`import`.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mariafonseca.financeanalytics.R
import com.mariafonseca.financeanalytics.core.designsystem.Space16
import com.mariafonseca.financeanalytics.core.designsystem.Space24

/**
 * Placeholder entry point for the CSV import flow. The real multi-step wizard
 * (select/preview/validate/progress/done/error, see
 * docs/project/05_DESIGN_SYSTEM.md section 21) is out of scope for this PR —
 * this only proves the feature boundary and the navigation/state foundation
 * needed to enter it (see FinanceAnalyticsNavHost's onContinue).
 */
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Space24)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Space16, Alignment.CenterVertically),
        ) {
            Text(
                text = stringResource(R.string.import_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.import_body),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.action_continue))
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.action_back))
            }
        }
    }
}
