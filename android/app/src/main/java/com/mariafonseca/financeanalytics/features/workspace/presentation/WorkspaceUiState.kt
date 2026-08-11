package com.mariafonseca.financeanalytics.features.workspace.presentation

import androidx.annotation.StringRes
import com.mariafonseca.financeanalytics.R

data class WorkspaceUiState(
    @param:StringRes val emptyStateHeadlineRes: Int = R.string.empty_state_headline,
    @param:StringRes val emptyStateBodyRes: Int = R.string.empty_state_body,
    val privacyPointsRes: List<Int> = listOf(
        R.string.privacy_point_on_device,
        R.string.privacy_point_no_bank,
        R.string.privacy_point_offline,
    ),
)
