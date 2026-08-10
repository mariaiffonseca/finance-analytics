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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mariafonseca.financeanalytics.core.designsystem.LocalFinanceColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun EmptyScreen(
    onImportClick: () -> Unit,
    viewModel: WorkspaceViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalFinanceColors.current

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "FINANCE ANALYTICS",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                color = colors.accent,
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = uiState.emptyStateHeadline,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = uiState.emptyStateBody,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(18.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.privacyPoints.forEach { point ->
                    PrivacyPointRow(text = point)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onImportClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
            ) {
                Text(
                    text = "Import CSV",
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
                .padding(top = 7.dp)
                .size(6.dp)
                .background(
                    color = colors.text,
                    shape = RoundedCornerShape(1.dp),
                ),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 13.sp,
            color = colors.textSecondary,
        )
    }
}
