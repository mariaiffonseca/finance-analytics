package com.mariafonseca.financeanalytics.features.workspace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mariafonseca.financeanalytics.R
import com.mariafonseca.financeanalytics.core.designsystem.LocalFinanceColors
import com.mariafonseca.financeanalytics.core.designsystem.Space8
import com.mariafonseca.financeanalytics.core.designsystem.Space12
import com.mariafonseca.financeanalytics.core.designsystem.Space20
import com.mariafonseca.financeanalytics.core.designsystem.Space24
import org.koin.androidx.compose.koinViewModel

@Composable
fun EmptyScreen(
    onImportClick: () -> Unit,
    viewModel: WorkspaceViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalFinanceColors.current

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Space24)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.empty_state_eyebrow),
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
            )

            Spacer(modifier = Modifier.height(Space20))

            Text(
                text = stringResource(uiState.emptyStateHeadlineRes),
                style = MaterialTheme.typography.headlineLarge,
            )

            Spacer(modifier = Modifier.height(Space12))

            Text(
                text = stringResource(uiState.emptyStateBodyRes),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )

            Spacer(modifier = Modifier.height(Space20))
            HorizontalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(Space20))

            Column(verticalArrangement = Arrangement.spacedBy(Space12)) {
                uiState.privacyPointsRes.forEach { pointRes ->
                    PrivacyPointRow(text = stringResource(pointRes))
                }
            }

            Spacer(modifier = Modifier.height(Space24))

            Button(
                onClick = onImportClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
            ) {
                Text(
                    text = stringResource(R.string.action_import_csv),
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PrivacyPointRow(text: String) {
    val colors = LocalFinanceColors.current
    Row(verticalAlignment = Alignment.Top) {
        Spacer(
            modifier = Modifier
                .padding(top = Space8)
                .size(6.dp)
                .background(
                    color = colors.text,
                    shape = RoundedCornerShape(1.dp),
                ),
        )
        Spacer(modifier = Modifier.width(Space8))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
    }
}
