package com.mariafonseca.financeanalytics.features.transactions.presentation

import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import java.time.LocalDate

/**
 * Search (merchant substring, case-insensitive) + category filtering for
 * the Transactions screen (docs/project/05_DESIGN_SYSTEM.md §18). Kept as
 * a pure function in the presentation layer, the same convention
 * InsightsFiltering.kt established, rather than inline inside a Composable.
 */
fun List<Transaction>.filteredBy(searchQuery: String, category: String?): List<Transaction> {
    val trimmedQuery = searchQuery.trim()
    return filter { transaction ->
        (category == null || transaction.category == category) &&
            (trimmedQuery.isEmpty() || transaction.merchant.contains(trimmedQuery, ignoreCase = true))
    }
}

/**
 * Groups transactions by date, most recent date first — the "date groups"
 * hierarchy design system §18 calls for. Within a date, order is preserved
 * from [this] (callers pass an already id/date-ordered list).
 */
fun List<Transaction>.groupedByDateDescending(): List<Pair<LocalDate, List<Transaction>>> =
    sortedByDescending { it.date }
        .groupBy { it.date }
        .toList()
