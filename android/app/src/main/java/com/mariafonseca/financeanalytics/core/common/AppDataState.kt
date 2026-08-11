package com.mariafonseca.financeanalytics.core.common

/**
 * Whether the app has financial data to analyse yet. Drives the phase-level
 * split between the import flow and the analytics workspace (see
 * docs/execution/01_ANALYTICS_WORKSPACE/PR-004_ANALYTICS_WORKSPACE.md,
 * "Application State"). No persistence yet — this only models the concept
 * in memory until a real data source lands.
 */
sealed interface AppDataState {
    data object NoData : AppDataState
    data object DataAvailable : AppDataState
}
