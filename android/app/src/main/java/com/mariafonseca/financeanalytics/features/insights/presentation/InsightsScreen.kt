package com.mariafonseca.financeanalytics.features.insights.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mariafonseca.financeanalytics.R
import com.mariafonseca.financeanalytics.core.analytics.model.Anomaly
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsSummary
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsUiState
import com.mariafonseca.financeanalytics.core.analytics.model.Insight
import com.mariafonseca.financeanalytics.core.analytics.model.RecurringTransaction
import com.mariafonseca.financeanalytics.core.common.formatMajorUnits
import com.mariafonseca.financeanalytics.core.designsystem.FinanceColors
import com.mariafonseca.financeanalytics.core.designsystem.LocalFinanceColors
import com.mariafonseca.financeanalytics.core.designsystem.Space12
import com.mariafonseca.financeanalytics.core.designsystem.Space24
import com.mariafonseca.financeanalytics.core.designsystem.Space8
import com.mariafonseca.financeanalytics.core.designsystem.component.FinanceFilterChip
import org.koin.androidx.compose.koinViewModel

@Composable
fun InsightsScreen(viewModel: InsightsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsContent(
        uiState = uiState,
        onFilterSelected = viewModel::onFilterSelected,
        onRetryAnalysis = viewModel::onRetryAnalysis,
    )
}

/**
 * State-driven body, deliberately free of any ViewModel/DI dependency so it
 * can be composed directly in a Compose test with a hand-built [InsightsUiState]
 * (see InsightsScreenTest) instead of requiring a full Koin graph + real
 * network stack just to exercise a UI state.
 */
@Composable
fun InsightsContent(
    uiState: InsightsUiState,
    onFilterSelected: (InsightFilter) -> Unit,
    onRetryAnalysis: () -> Unit,
) {
    val colors = LocalFinanceColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Space24)
            // Success can render a long insights/anomalies/recurring list;
            // the outer Column has no fixed height to scroll within
            // otherwise (matches ImportScreen's own verticalScroll usage).
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(uiState.titleRes),
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(Space24))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Space8),
            modifier = Modifier.selectableGroup(),
        ) {
            items(uiState.filters) { filter ->
                val onFilterClick = remember(filter) { { onFilterSelected(filter) } }
                FinanceFilterChip(
                    label = stringResource(filter.labelRes),
                    selected = filter == uiState.selectedFilter,
                    onClick = onFilterClick,
                )
            }
        }

        Spacer(modifier = Modifier.height(Space24))

        when (val analyticsState = uiState.analyticsState) {
            AnalyticsUiState.Idle -> Text(
                text = stringResource(uiState.placeholderMessageRes),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )
            AnalyticsUiState.Loading -> LoadingContent()
            is AnalyticsUiState.Success -> AnalyticsContent(
                view = analyticsState.result.viewFor(uiState.selectedFilter),
                isFiltered = uiState.selectedFilter != InsightFilter.RECENT,
            )
            AnalyticsUiState.Unavailable -> AnalyticsMessageContent(
                titleRes = R.string.analytics_unavailable_title,
                messageRes = R.string.analytics_unavailable_message,
                onRetry = onRetryAnalysis,
            )
            AnalyticsUiState.Error -> AnalyticsMessageContent(
                titleRes = R.string.analytics_error_title,
                messageRes = R.string.analytics_error_message,
                onRetry = onRetryAnalysis,
                background = colors.errorTint,
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    val colors = LocalFinanceColors.current
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = colors.accent)
    }
}

@Composable
private fun AnalyticsMessageContent(
    titleRes: Int,
    messageRes: Int,
    onRetry: () -> Unit,
    background: Color? = null,
) {
    val colors = LocalFinanceColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (background != null) it.background(background) else it }
            .padding(if (background != null) Space12 else 0.dp),
        verticalArrangement = Arrangement.spacedBy(Space12),
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleLarge,
            color = if (background != null) colors.accentDeep else colors.text,
        )
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
        )
        OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(0.dp)) {
            Text(text = stringResource(R.string.action_try_again))
        }
    }
}

/**
 * Renders one filtered [InsightsView] (PR-015 §7). A `null` section is
 * excluded from a narrow filter (Spending/Trends/Anomalies/Recurring) and
 * is not rendered at all; when every included section has no data, a
 * single combined empty state is shown instead of stacking each section's
 * own empty message — the Recent/all view (`isFiltered = false`) keeps the
 * original per-section empty messages, since there each section is always
 * relevant regardless of what the others contain.
 */
@Composable
private fun AnalyticsContent(view: InsightsView, isFiltered: Boolean) {
    val visibleSections = listOfNotNull(view.insights, view.anomalies, view.recurring)
    val filteredResultsAreEmpty = isFiltered && visibleSections.all { it.isEmpty() }

    Column(verticalArrangement = Arrangement.spacedBy(Space24)) {
        SummarySection(view.summary)
        if (filteredResultsAreEmpty) {
            EmptySectionMessage(R.string.analytics_no_filtered_insights)
        } else {
            view.insights?.let { InsightsSection(it) }
            view.anomalies?.let { AnomaliesSection(it) }
            view.recurring?.let { RecurringSection(it) }
        }
    }
}

@Composable
private fun SummarySection(summary: AnalyticsSummary) {
    val colors = LocalFinanceColors.current
    Column {
        Text(
            text = stringResource(R.string.analytics_summary_total_spent),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
        )
        Text(
            text = formatMajorUnits(summary.totalExpenses),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun InsightsSection(insights: List<Insight>) {
    val colors = LocalFinanceColors.current
    Column {
        Text(text = stringResource(R.string.analytics_section_insights), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(Space12))
        if (insights.isEmpty()) {
            EmptySectionMessage(R.string.analytics_no_insights)
        } else {
            insights.forEach { insight ->
                InsightRow(insight)
                HorizontalDivider(color = colors.divider)
            }
        }
    }
}

@Composable
private fun InsightRow(insight: Insight) {
    val colors = LocalFinanceColors.current
    val dotColor = insightAccentColor(insight.type, colors)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = Space12)) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .background(color = dotColor, shape = CircleShape),
        )
        Spacer(modifier = Modifier.width(Space8))
        Column {
            Text(text = insight.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(text = insight.description, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}

private fun insightAccentColor(type: String, colors: FinanceColors): Color = when (type.lowercase()) {
    "positive" -> colors.positive
    "negative" -> colors.accent
    "anomaly" -> colors.warningDeep
    else -> colors.textSecondary
}

@Composable
private fun AnomaliesSection(anomalies: List<Anomaly>) {
    val colors = LocalFinanceColors.current
    Column {
        Text(text = stringResource(R.string.analytics_section_anomalies), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(Space12))
        if (anomalies.isEmpty()) {
            EmptySectionMessage(R.string.analytics_no_anomalies)
        } else {
            anomalies.forEach { anomaly ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = Space12)) {
                    Text(text = anomaly.reason, style = MaterialTheme.typography.bodyLarge)
                    Text(text = anomaly.method, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
                HorizontalDivider(color = colors.divider)
            }
        }
    }
}

@Composable
private fun RecurringSection(recurring: List<RecurringTransaction>) {
    val colors = LocalFinanceColors.current
    Column {
        Text(text = stringResource(R.string.analytics_section_recurring), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(Space12))
        if (recurring.isEmpty()) {
            EmptySectionMessage(R.string.analytics_no_recurring)
        } else {
            recurring.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Space12),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(text = item.merchant, style = MaterialTheme.typography.bodyLarge)
                        Text(text = item.classification, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                    item.medianAmount?.let { amount ->
                        Text(text = formatMajorUnits(amount), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                HorizontalDivider(color = colors.divider)
            }
        }
    }
}

@Composable
private fun EmptySectionMessage(@StringRes messageRes: Int) {
    val colors = LocalFinanceColors.current
    Text(text = stringResource(messageRes), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
}
