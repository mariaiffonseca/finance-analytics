package com.mariafonseca.financeanalytics.core.analytics.dto

import kotlinx.serialization.Serializable

/** Mirrors `AnalysisRequest` / `TransactionRequest` in analytics/src/finance_analytics/api/schemas.py (PR-013 §5). */
@Serializable
data class AnalyticsRequestDto(
    val transactions: List<TransactionRequestDto>,
)

@Serializable
data class TransactionRequestDto(
    val id: String,
    val date: String,
    val amount: Double,
    val currency: String,
    val description: String? = null,
    val merchant: String,
    val category: String? = null,
    val account: String? = null,
)
