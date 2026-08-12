package com.mariafonseca.financeanalytics.features.transactions.data

import com.mariafonseca.financeanalytics.core.common.Money
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TransactionMapperTest {

    @Test
    fun `entity maps to domain`() {
        val entity = TransactionEntity(
            id = 7,
            date = LocalDate.of(2026, 8, 1),
            merchant = "Coffee Shop",
            amountMinorUnits = -450,
            category = "Food",
        )

        val domain = entity.toDomain()

        assertEquals(
            Transaction(
                id = 7,
                date = LocalDate.of(2026, 8, 1),
                merchant = "Coffee Shop",
                amount = Money(-450),
                category = "Food",
            ),
            domain,
        )
    }

    @Test
    fun `domain maps to entity`() {
        val domain = Transaction(
            id = 7,
            date = LocalDate.of(2026, 8, 1),
            merchant = "Coffee Shop",
            amount = Money(-450),
            category = "Food",
        )

        val entity = domain.toEntity()

        assertEquals(
            TransactionEntity(
                id = 7,
                date = LocalDate.of(2026, 8, 1),
                merchant = "Coffee Shop",
                amountMinorUnits = -450,
                category = "Food",
            ),
            entity,
        )
    }

    @Test
    fun `mapping to entity and back preserves the transaction`() {
        val original = Transaction(
            id = 3,
            date = LocalDate.of(2026, 7, 15),
            merchant = "Grocery Store",
            amount = Money(1250),
            category = null,
        )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original, roundTripped)
    }
}
