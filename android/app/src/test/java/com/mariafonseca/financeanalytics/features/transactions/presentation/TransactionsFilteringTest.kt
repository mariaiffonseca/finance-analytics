package com.mariafonseca.financeanalytics.features.transactions.presentation

import com.mariafonseca.financeanalytics.core.common.Money
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TransactionsFilteringTest {

    private fun transaction(id: Long, merchant: String, category: String?, date: LocalDate = LocalDate.of(2026, 3, 1)) =
        Transaction(id = id, date = date, merchant = merchant, amount = Money(-100), category = category)

    private val transactions = listOf(
        transaction(1, "Coffee Corner", "Food"),
        transaction(2, "Continente", "Groceries"),
        transaction(3, "Café Central", "Food"),
    )

    @Test
    fun `no filters returns every transaction`() {
        assertEquals(transactions, transactions.filteredBy(searchQuery = "", category = null))
    }

    @Test
    fun `search filters by merchant substring case-insensitively`() {
        val result = transactions.filteredBy(searchQuery = "coffee", category = null)

        assertEquals(listOf(transactions[0]), result)
    }

    @Test
    fun `search query is trimmed`() {
        val result = transactions.filteredBy(searchQuery = "  coffee  ", category = null)

        assertEquals(listOf(transactions[0]), result)
    }

    @Test
    fun `category filter narrows to the exact matching category`() {
        val result = transactions.filteredBy(searchQuery = "", category = "Food")

        assertEquals(listOf(transactions[0], transactions[2]), result)
    }

    @Test
    fun `search and category filters combine`() {
        val result = transactions.filteredBy(searchQuery = "central", category = "Food")

        assertEquals(listOf(transactions[2]), result)
    }

    @Test
    fun `no matches yields an empty list`() {
        val result = transactions.filteredBy(searchQuery = "nonexistent", category = null)

        assertEquals(emptyList<Transaction>(), result)
    }

    @Test
    fun `grouping orders dates most recent first`() {
        val grouped = listOf(
            transaction(1, "A", "Food", LocalDate.of(2026, 3, 1)),
            transaction(2, "B", "Food", LocalDate.of(2026, 3, 5)),
            transaction(3, "C", "Food", LocalDate.of(2026, 3, 3)),
        ).groupedByDateDescending()

        assertEquals(
            listOf(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 1)),
            grouped.map { it.first },
        )
    }

    @Test
    fun `grouping keeps same-date transactions together`() {
        val sameDate = LocalDate.of(2026, 3, 1)
        val grouped = listOf(
            transaction(1, "A", "Food", sameDate),
            transaction(2, "B", "Food", sameDate),
        ).groupedByDateDescending()

        assertEquals(1, grouped.size)
        assertEquals(2, grouped.first().second.size)
    }
}
