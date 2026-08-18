package com.mariafonseca.financeanalytics.features.overview.presentation

import com.mariafonseca.financeanalytics.core.common.Money
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MonthlyOverviewTest {

    private fun transaction(
        id: Long,
        date: LocalDate,
        amount: Long,
        category: String? = "Food",
    ) = Transaction(id = id, date = date, merchant = "Merchant $id", amount = Money(amount), category = category)

    @Test
    fun `empty transaction list yields no overview`() {
        assertNull(emptyList<Transaction>().toMonthlyOverview())
    }

    @Test
    fun `overview is built for the most recent month present in the data`() {
        val transactions = listOf(
            transaction(1, LocalDate.of(2026, 1, 15), -1000),
            transaction(2, LocalDate.of(2026, 3, 5), -2000),
        )

        val overview = transactions.toMonthlyOverview()

        assertEquals("March 2026", overview?.monthLabel)
        assertEquals(20.0, overview?.totalSpent)
    }

    @Test
    fun `total spent, income and saved are computed for the latest month only`() {
        val transactions = listOf(
            // February — should not affect the March figures.
            transaction(1, LocalDate.of(2026, 2, 1), -50_00),
            transaction(2, LocalDate.of(2026, 2, 1), 500_00),
            // March — the latest month.
            transaction(3, LocalDate.of(2026, 3, 1), -30_00),
            transaction(4, LocalDate.of(2026, 3, 5), 200_00),
        )

        val overview = transactions.toMonthlyOverview()!!

        assertEquals(30.0, overview.totalSpent, 0.001)
        assertEquals(200.0, overview.totalIncome, 0.001)
        assertEquals(170.0, overview.totalSaved, 0.001)
    }

    @Test
    fun `category breakdown groups expenses by category with correct shares`() {
        val transactions = listOf(
            transaction(1, LocalDate.of(2026, 3, 1), -75_00, category = "Groceries"),
            transaction(2, LocalDate.of(2026, 3, 2), -25_00, category = "Transport"),
            // Income never contributes to the category breakdown.
            transaction(3, LocalDate.of(2026, 3, 3), 500_00, category = "Income"),
        )

        val breakdown = transactions.toMonthlyOverview()!!.categoryBreakdown

        assertEquals(listOf("Groceries", "Transport"), breakdown.map { it.category })
        assertEquals(0.75, breakdown[0].share, 0.001)
        assertEquals(0.25, breakdown[1].share, 0.001)
    }

    @Test
    fun `blank or missing category falls back to Uncategorised`() {
        val transactions = listOf(transaction(1, LocalDate.of(2026, 3, 1), -10_00, category = null))

        val breakdown = transactions.toMonthlyOverview()!!.categoryBreakdown

        assertEquals(listOf("Uncategorised"), breakdown.map { it.category })
    }

    @Test
    fun `monthly trend covers up to six most recent months in order`() {
        val transactions = (1..8).map { month ->
            transaction(month.toLong(), LocalDate.of(2026, month, 1), -10_00L * month)
        }

        val trend = transactions.toMonthlyOverview()!!.monthlyTrend

        assertEquals(6, trend.size)
        assertEquals(30.0, trend.first().totalSpent, 0.001) // March, the 6th-most-recent month
        assertEquals(80.0, trend.last().totalSpent, 0.001) // August, the most recent
    }

    @Test
    fun `income transactions are excluded from the category breakdown total`() {
        val transactions = listOf(transaction(1, LocalDate.of(2026, 3, 1), 100_00, category = "Income"))

        val overview = transactions.toMonthlyOverview()!!

        assertTrue(overview.categoryBreakdown.isEmpty())
    }
}
