package com.mariafonseca.financeanalytics.core.analytics.model

/**
 * ViewModel-level analytics request state (PR-014 §10). Shared under
 * core/analytics rather than one feature's presentation package since more
 * than one screen (Insights today, Overview later) can trigger the same
 * kind of request and needs the same vocabulary.
 *
 * Unavailable vs. Error: Unavailable covers "the API isn't reachable or
 * working right now" (no connection, timeout, HTTP 5xx) — expected in local
 * development and handled per PR-014 §8 without disrupting the rest of the
 * app. Error covers "something about the request or response itself was
 * wrong" (HTTP 4xx, a malformed body, or anything unexpected) — still
 * surfaced without raw technical detail (PR-014 §7), but distinct because
 * retrying the exact same request is less likely to help on its own.
 */
sealed interface AnalyticsUiState {
    data object Idle : AnalyticsUiState
    data object Loading : AnalyticsUiState
    data class Success(val result: AnalyticsResult) : AnalyticsUiState
    data object Unavailable : AnalyticsUiState
    data object Error : AnalyticsUiState
}

/**
 * Shared `Result<AnalyticsResult>` -> [AnalyticsUiState] mapping, used by
 * every ViewModel that triggers an analytics request (InsightsViewModel,
 * OverviewViewModel) so the Unavailable/Error distinction stays consistent
 * across screens instead of being reimplemented per ViewModel.
 */
fun Result<AnalyticsResult>.toAnalyticsUiState(): AnalyticsUiState {
    getOrNull()?.let { return AnalyticsUiState.Success(it) }
    val reason = (exceptionOrNull() as? AnalyticsApiException)?.reason ?: AnalyticsFailureReason.UNEXPECTED
    return when (reason) {
        AnalyticsFailureReason.NO_CONNECTION,
        AnalyticsFailureReason.TIMEOUT,
        AnalyticsFailureReason.SERVER_ERROR,
        -> AnalyticsUiState.Unavailable

        AnalyticsFailureReason.CLIENT_ERROR,
        AnalyticsFailureReason.INVALID_RESPONSE,
        AnalyticsFailureReason.UNEXPECTED,
        -> AnalyticsUiState.Error
    }
}
