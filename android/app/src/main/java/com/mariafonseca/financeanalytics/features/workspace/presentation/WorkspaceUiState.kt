package com.mariafonseca.financeanalytics.features.workspace.presentation

data class WorkspaceUiState(
    val emptyStateHeadline: String = "Understand where your money goes.",
    val emptyStateBody: String = "Import a CSV export from your bank and see " +
        "patterns, trends and anomalies in your spending. No manual entry.",
    val privacyPoints: List<String> = listOf(
        "Everything is analysed on this device",
        "No bank connection or login required",
        "Works fully offline once imported",
    ),
)
