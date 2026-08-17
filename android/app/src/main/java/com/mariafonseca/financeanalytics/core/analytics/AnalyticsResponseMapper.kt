package com.mariafonseca.financeanalytics.core.analytics

import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsResponseDto
import com.mariafonseca.financeanalytics.core.analytics.dto.AnalyticsSummaryDto
import com.mariafonseca.financeanalytics.core.analytics.dto.AnomalyDto
import com.mariafonseca.financeanalytics.core.analytics.dto.InsightDto
import com.mariafonseca.financeanalytics.core.analytics.dto.RecurringDto
import com.mariafonseca.financeanalytics.core.analytics.model.Anomaly
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsResult
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsSummary
import com.mariafonseca.financeanalytics.core.analytics.model.Insight
import com.mariafonseca.financeanalytics.core.analytics.model.RecurringTransaction
import java.time.LocalDate

fun AnalyticsResponseDto.toDomain(): AnalyticsResult = AnalyticsResult(
    summary = summary.toDomain(),
    insights = insights.map { it.toDomain() },
    anomalies = anomalies.map { it.toDomain() },
    recurring = recurring.map { it.toDomain() },
)

fun AnalyticsSummaryDto.toDomain(): AnalyticsSummary = AnalyticsSummary(
    transactionCount = transactionCount,
    incomeCount = incomeCount,
    expenseCount = expenseCount,
    totalIncome = totalIncome,
    totalExpenses = totalExpenses,
    netSavings = netSavings,
    dateRangeStart = dateRangeStart?.let(LocalDate::parse),
    dateRangeEnd = dateRangeEnd?.let(LocalDate::parse),
    uniqueMerchantCount = uniqueMerchantCount,
    uniqueCategoryCount = uniqueCategoryCount,
)

fun InsightDto.toDomain(): Insight = Insight(
    id = id,
    type = type,
    title = title,
    description = description,
    severity = severity,
    confidence = confidence,
    relatedTransactionIds = relatedTransactionIds,
    category = category,
    merchant = merchant,
    amount = amount,
    comparisonPeriod = comparisonPeriod,
)

fun AnomalyDto.toDomain(): Anomaly = Anomaly(
    transactionId = transactionId,
    anomalyScore = anomalyScore,
    isAnomaly = isAnomaly,
    method = method,
    reason = reason,
)

fun RecurringDto.toDomain(): RecurringTransaction = RecurringTransaction(
    merchant = merchant,
    currency = currency,
    isRecurring = isRecurring,
    classification = classification,
    confidenceScore = confidenceScore,
    frequency = frequency,
    occurrences = occurrences,
    medianAmount = medianAmount,
    amountVariation = amountVariation,
    medianIntervalDays = medianIntervalDays,
    intervalVariation = intervalVariation,
    firstSeen = LocalDate.parse(firstSeen),
    lastSeen = LocalDate.parse(lastSeen),
    reason = reason,
    transactionIds = transactionIds,
)
