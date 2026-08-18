package com.mariafonseca.financeanalytics.features.overview.presentation

import androidx.annotation.StringRes
import com.mariafonseca.financeanalytics.R
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsUiState

data class OverviewUiState(
    @param:StringRes val titleRes: Int = R.string.tab_overview,
    @param:StringRes val placeholderMessageRes: Int = R.string.overview_placeholder_message,
    val monthlyOverview: MonthlyOverview? = null,
    val analyticsState: AnalyticsUiState = AnalyticsUiState.Idle,
)
