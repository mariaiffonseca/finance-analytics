package com.mariafonseca.financeanalytics.core.analytics

import com.mariafonseca.financeanalytics.core.common.Money
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AnalyticsRequestMapperTest {

    private val transaction = Transaction(
        id = 42,
        date = LocalDate.of(2026, 8, 1),
        merchant = "Coffee Shop",
        amount = Money(-450),
        category = "Food",
    )

    @Test
    fun `id is converted to a non-blank string`() {
        assertEquals("42", transaction.toRequestDto().id)
    }

    @Test
    fun `date is formatted as ISO-8601 yyyy-MM-dd`() {
        assertEquals("2026-08-01", transaction.toRequestDto().date)
    }

    @Test
    fun `amount is converted from minor units to a signed major-unit value`() {
        assertEquals(-4.50, transaction.toRequestDto().amount, 0.0001)
    }

    @Test
    fun `currency defaults since the domain model does not track it yet`() {
        assertEquals("EUR", transaction.toRequestDto().currency)
    }

    @Test
    fun `category is passed through`() {
        assertEquals("Food", transaction.toRequestDto().category)
    }

    @Test
    fun `null category maps to null`() {
        val dto = transaction.copy(category = null).toRequestDto()
        assertNull(dto.category)
    }

    @Test
    fun `description and account are always null`() {
        val dto = transaction.toRequestDto()
        assertNull(dto.description)
        assertNull(dto.account)
    }

    @Test
    fun `merchant is passed through`() {
        assertEquals("Coffee Shop", transaction.toRequestDto().merchant)
    }

    @Test
    fun `a list of transactions maps to a request with one entry per transaction`() {
        val other = transaction.copy(id = 43, merchant = "Grocer")
        val request = listOf(transaction, other).toRequestDto()

        assertEquals(2, request.transactions.size)
        assertEquals("42", request.transactions[0].id)
        assertEquals("43", request.transactions[1].id)
    }

    @Test
    fun `an empty list maps to an empty request`() {
        assertEquals(emptyList<Any>(), emptyList<Transaction>().toRequestDto().transactions)
    }
}
