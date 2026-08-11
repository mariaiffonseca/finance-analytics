package com.mariafonseca.financeanalytics.features.overview.presentation

import androidx.annotation.StringRes
import com.mariafonseca.financeanalytics.R

data class OverviewUiState(
    @param:StringRes val titleRes: Int = R.string.overview_title,
    @param:StringRes val placeholderMessageRes: Int = R.string.overview_placeholder_message,
)
