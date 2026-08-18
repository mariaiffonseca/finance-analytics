package com.mariafonseca.financeanalytics.features.overview.presentation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mariafonseca.financeanalytics.R
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsUiState
import com.mariafonseca.financeanalytics.core.analytics.model.Insight
import com.mariafonseca.financeanalytics.core.common.formatMajorUnits
import com.mariafonseca.financeanalytics.core.designsystem.LocalFinanceColors
import com.mariafonseca.financeanalytics.core.designsystem.Space12
import com.mariafonseca.financeanalytics.core.designsystem.Space24
import com.mariafonseca.financeanalytics.core.designsystem.Space8
import com.mariafonseca.financeanalytics.core.designsystem.chart.CategoryDonutChart
import com.mariafonseca.financeanalytics.core.designsystem.chart.SpendingTrendChart
import com.mariafonseca.financeanalytics.core.designsystem.chart.categorySliceColor
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@Composable
fun OverviewScreen(viewModel: OverviewViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OverviewContent(uiState = uiState, onRetryAnalysis = viewModel::onRetryAnalysis)
}

/**
 * State-driven body, free of ViewModel/DI dependencies for the same reason
 * as InsightsContent (see InsightsScreen.kt) — directly composable in a
 * Compose test with a hand-built [OverviewUiState].
 */
@Composable
fun OverviewContent(uiState: OverviewUiState, onRetryAnalysis: () -> Unit) {
    val colors = LocalFinanceColors.current
    val overview = uiState.monthlyOverview

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Space24)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(text = stringResource(uiState.titleRes), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(Space24))

        if (overview == null) {
            Text(
                text = stringResource(uiState.placeholderMessageRes),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )
        } else {
            Text(
                text = overview.monthLabel,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(Space12))

            TotalSpentMetric(overview.totalSpent)
            Spacer(modifier = Modifier.height(Space24))

            IncomeSavedRow(overview.totalIncome, overview.totalSaved)
            Spacer(modifier = Modifier.height(Space24))
            HorizontalDivider(color = colors.divider)
            Spacer(modifier = Modifier.height(Space24))

            InsightsPreviewSection(uiState.analyticsState, onRetryAnalysis)
            Spacer(modifier = Modifier.height(Space24))

            if (overview.monthlyTrend.size > 1) {
                SpendingTrendSection(overview.monthlyTrend)
                Spacer(modifier = Modifier.height(Space24))
            }

            if (overview.categoryBreakdown.isNotEmpty()) {
                CategoryBreakdownSection(overview.categoryBreakdown)
            }

            MoreInsightsSection(uiState.analyticsState)
        }
    }
}

@Composable
private fun TotalSpentMetric(totalSpent: Double) {
    val colors = LocalFinanceColors.current
    Column {
        Text(
            text = stringResource(R.string.analytics_summary_total_spent),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
        )
        Text(text = formatMajorUnits(totalSpent), style = MaterialTheme.typography.displayLarge)
    }
}

@Composable
private fun IncomeSavedRow(totalIncome: Double, totalSaved: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space24)) {
        MetricBlock(labelRes = R.string.overview_income_label, value = totalIncome, modifier = Modifier.weight(1f))
        MetricBlock(labelRes = R.string.overview_saved_label, value = totalSaved, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MetricBlock(@StringRes labelRes: Int, value: Double, modifier: Modifier = Modifier) {
    val colors = LocalFinanceColors.current
    Column(modifier = modifier) {
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        Text(text = formatMajorUnits(value), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun InsightsPreviewSection(analyticsState: AnalyticsUiState, onRetryAnalysis: () -> Unit) {
    when (analyticsState) {
        AnalyticsUiState.Idle -> Unit
        AnalyticsUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
            val colors = LocalFinanceColors.current
            CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(Space8))
            Text(text = stringResource(R.string.tab_insights), style = MaterialTheme.typography.titleLarge)
        }
        is AnalyticsUiState.Success -> TopInsightContent(analyticsState.result.insights)
        AnalyticsUiState.Unavailable -> InsightsPreviewUnavailable(
            titleRes = R.string.analytics_unavailable_title,
            messageRes = R.string.analytics_unavailable_message,
            onRetry = onRetryAnalysis,
        )
        AnalyticsUiState.Error -> InsightsPreviewUnavailable(
            titleRes = R.string.analytics_error_title,
            messageRes = R.string.analytics_error_message,
            onRetry = onRetryAnalysis,
        )
    }
}

@Composable
private fun TopInsightContent(insights: List<Insight>) {
    val colors = LocalFinanceColors.current
    if (insights.isEmpty()) {
        Text(text = stringResource(R.string.analytics_no_insights), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        return
    }

    Text(text = stringResource(R.string.overview_top_insight_label), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
    Spacer(modifier = Modifier.height(Space8))
    val topInsight = insights.first()
    Text(text = topInsight.title, style = MaterialTheme.typography.titleLarge)
    Text(text = topInsight.description, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
}

/**
 * Rendered last, after the trend/breakdown charts (docs/project/05_DESIGN_SYSTEM.md
 * §16, items 6-8: spending trend and category breakdown come before "more
 * insights") — only [TopInsightContent] renders above the charts.
 */
@Composable
private fun MoreInsightsSection(analyticsState: AnalyticsUiState) {
    val remaining = (analyticsState as? AnalyticsUiState.Success)?.result?.insights?.drop(1).orEmpty()
    if (remaining.isEmpty()) return

    val colors = LocalFinanceColors.current
    Spacer(modifier = Modifier.height(Space24))
    Text(text = stringResource(R.string.overview_more_insights_label), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
    Spacer(modifier = Modifier.height(Space8))
    remaining.forEach { insight ->
        Text(text = insight.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(text = insight.description, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Spacer(modifier = Modifier.height(Space12))
    }
}

@Composable
private fun InsightsPreviewUnavailable(@StringRes titleRes: Int, @StringRes messageRes: Int, onRetry: () -> Unit) {
    val colors = LocalFinanceColors.current
    Column(verticalArrangement = Arrangement.spacedBy(Space8)) {
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleLarge)
        Text(text = stringResource(messageRes), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        OutlinedButton(onClick = onRetry) {
            Text(text = stringResource(R.string.action_try_again))
        }
    }
}

@Composable
private fun SpendingTrendSection(monthlyTrend: List<MonthlyTrendPoint>) {
    val colors = LocalFinanceColors.current
    // The chart is a bare Canvas with no semantics of its own, and unlike
    // CategoryBreakdownSection's donut (whose legend already exposes the
    // same figures as text), nothing else on screen states these monthly
    // totals as text — so a screen reader would otherwise skip this data
    // entirely (docs/project/05_DESIGN_SYSTEM.md §27, "accessible
    // alternatives for charts").
    val chartDescription = monthlyTrend.joinToString(", ") { "${it.monthLabel} ${formatMajorUnits(it.totalSpent)}" }
    Column {
        Text(text = stringResource(R.string.overview_spending_trend_label), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        Spacer(modifier = Modifier.height(Space12))
        SpendingTrendChart(
            values = monthlyTrend.map { it.totalSpent },
            modifier = Modifier.semantics { contentDescription = chartDescription },
        )
        Spacer(modifier = Modifier.height(Space8))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            monthlyTrend.forEach { point ->
                Text(text = point.monthLabel, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun CategoryBreakdownSection(categoryBreakdown: List<CategorySlice>) {
    val colors = LocalFinanceColors.current
    Column {
        Text(text = stringResource(R.string.overview_category_breakdown_label), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        Spacer(modifier = Modifier.height(Space12))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryDonutChart(slices = categoryBreakdown)
            Spacer(modifier = Modifier.width(Space24))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space8)) {
                categoryBreakdown.forEachIndexed { index, slice ->
                    CategoryLegendRow(slice = slice, swatchColor = categorySliceColor(index, colors))
                }
            }
        }
    }
}

@Composable
private fun CategoryLegendRow(slice: CategorySlice, swatchColor: Color) {
    val colors = LocalFinanceColors.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color = swatchColor, shape = CircleShape))
        Spacer(modifier = Modifier.width(Space8))
        Text(text = slice.category, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            text = "${(slice.share * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
    }
}
