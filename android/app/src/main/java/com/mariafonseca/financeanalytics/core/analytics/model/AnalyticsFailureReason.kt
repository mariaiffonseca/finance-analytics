package com.mariafonseca.financeanalytics.core.analytics.model

/** Categorized analytics network failure (PR-014 §7): no internet, timeout, HTTP 4xx/5xx, a malformed response, or anything unexpected. */
enum class AnalyticsFailureReason {
    NO_CONNECTION,
    TIMEOUT,
    CLIENT_ERROR,
    SERVER_ERROR,
    INVALID_RESPONSE,
    UNEXPECTED,
}

/**
 * Carries [reason] through [Result]'s Throwable slot so
 * AnalyticsRepositoryImpl's callers can distinguish failure categories
 * (e.g. to choose Unavailable vs Error UI state) without inspecting
 * exception types directly. Message is a fixed, non-technical description —
 * never a raw exception message — so it is safe to reference in logs
 * without risking payload leakage (PR-014 §17).
 */
class AnalyticsApiException(
    val reason: AnalyticsFailureReason,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
