package com.mariafonseca.financeanalytics.core.analytics

import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsRequestDto
import com.mariafonseca.financeanalytics.core.analytics.dto.TransactionRequestDto
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction

/**
 * The domain [Transaction] has no currency field yet — CSV import validates
 * a Currency column for cross-row consistency but never persists it (see
 * ImportRowValidator's doc comment: "deferred to the currency setting").
 * Every request transaction is sent as [DEFAULT_CURRENCY] until that lands.
 * This is a known limitation of the local domain model, not an invented API
 * field — `currency` is required by the actual PR-013 contract either way.
 */
private const val DEFAULT_CURRENCY = "EUR"
private const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0

fun List<Transaction>.toRequestDto(): AnalyticsRequestDto =
    AnalyticsRequestDto(transactions = map { it.toRequestDto() })

fun Transaction.toRequestDto(): TransactionRequestDto = TransactionRequestDto(
    id = id.toString(),
    date = date.toString(), // LocalDate.toString() is ISO-8601 yyyy-MM-dd, matching TransactionRequest.date.
    amount = amount.minorUnits / MINOR_UNITS_PER_MAJOR_UNIT,
    currency = DEFAULT_CURRENCY,
    description = null,
    merchant = merchant,
    category = category,
    account = null,
)
