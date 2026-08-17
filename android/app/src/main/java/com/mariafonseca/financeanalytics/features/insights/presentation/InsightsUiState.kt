package com.mariafonseca.financeanalytics.features.insights.presentation

import androidx.annotation.StringRes
import com.mariafonseca.financeanalytics.R
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsUiState

data class InsightsUiState(
    @param:StringRes val titleRes: Int = R.string.tab_insights,
    @param:StringRes val placeholderMessageRes: Int = R.string.insights_placeholder_message,
    val filters: List<InsightFilter> = InsightFilter.entries,
    val selectedFilter: InsightFilter = InsightFilter.RECENT,
    val analyticsState: AnalyticsUiState = AnalyticsUiState.Idle,
)
