package com.mariafonseca.financeanalytics.features.insights.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mariafonseca.financeanalytics.core.designsystem.LocalFinanceColors
import com.mariafonseca.financeanalytics.core.designsystem.Space24
import com.mariafonseca.financeanalytics.core.designsystem.Space8
import com.mariafonseca.financeanalytics.core.designsystem.component.FilterChip
import org.koin.androidx.compose.koinViewModel

@Composable
fun InsightsScreen(viewModel: InsightsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalFinanceColors.current

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Space24),
        ) {
            Text(
                text = stringResource(uiState.titleRes),
                style = MaterialTheme.typography.headlineSmall,
            )

            Spacer(modifier = Modifier.height(Space24))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(Space8)) {
                items(uiState.filters) { filter ->
                    FilterChip(
                        label = stringResource(filter.labelRes),
                        selected = filter == uiState.selectedFilter,
                        onClick = { viewModel.onFilterSelected(filter) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Space24))

            Text(
                text = stringResource(uiState.placeholderMessageRes),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )
        }
    }
}
