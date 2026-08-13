package com.mariafonseca.financeanalytics.features.`import`.data

import com.mariafonseca.financeanalytics.core.common.Money
import com.mariafonseca.financeanalytics.features.`import`.model.ImportRowErrorReason
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ImportRowValidatorTest {

    private val mappingWithoutOptionalColumns = ImportColumnMapping(
        dateColumn = 0,
        merchantColumn = 1,
        amountColumn = 2,
        categoryColumn = null,
        currencyColumn = null,
    )

    private val mappingWithCategory = mappingWithoutOptionalColumns.copy(categoryColumn = 3)

    private val mappingWithCurrency = mappingWithoutOptionalColumns.copy(currencyColumn = 3)

    @Test
    fun `valid row maps to a transaction`() {
        val result = ImportRowValidator().validate(
            rowNumber = 2,
            values = listOf("2026-08-01", "Coffee Shop", "-4.50", "Food"),
            mapping = mappingWithCategory,
        )

        assertEquals(
            ImportRowResult.Valid(
                Transaction(date = LocalDate.of(2026, 8, 1), merchant = "Coffee Shop", amount = Money(-450), category = "Food"),
            ),
            result,
        )
    }

    @Test
    fun `blank category becomes null`() {
        val result = ImportRowValidator().validate(
            rowNumber = 2,
            values = listOf("2026-08-01", "Coffee Shop", "-4.50", ""),
            mapping = mappingWithCategory,
        ) as ImportRowResult.Valid

        assertEquals(null, result.transaction.category)
    }

    @Test
    fun `missing date is invalid`() {
        val result = ImportRowValidator().validate(2, listOf("", "Coffee Shop", "-4.50"), mappingWithoutOptionalColumns)

        assertEquals(ImportRowErrorReason.MISSING_DATE, (result as ImportRowResult.Invalid).error.reason)
    }

    @Test
    fun `missing merchant is invalid`() {
        val result = ImportRowValidator().validate(2, listOf("2026-08-01", "", "-4.50"), mappingWithoutOptionalColumns)

        assertEquals(ImportRowErrorReason.MISSING_MERCHANT, (result as ImportRowResult.Invalid).error.reason)
    }

    @Test
    fun `missing amount is invalid`() {
        val result = ImportRowValidator().validate(2, listOf("2026-08-01", "Coffee Shop", ""), mappingWithoutOptionalColumns)

        assertEquals(ImportRowErrorReason.MISSING_AMOUNT, (result as ImportRowResult.Invalid).error.reason)
    }

    @Test
    fun `a row with fewer columns than the header is treated as a missing value, not a crash`() {
        val result = ImportRowValidator().validate(2, listOf("2026-08-01", "Coffee Shop"), mappingWithoutOptionalColumns)

        assertEquals(ImportRowErrorReason.MISSING_AMOUNT, (result as ImportRowResult.Invalid).error.reason)
    }

    @Test
    fun `non iso date is invalid`() {
        val result = ImportRowValidator().validate(2, listOf("01/08/2026", "Coffee Shop", "-4.50"), mappingWithoutOptionalColumns)

        assertEquals(ImportRowErrorReason.INVALID_DATE, (result as ImportRowResult.Invalid).error.reason)
    }

    @Test
    fun `non numeric amount is invalid`() {
        val result = ImportRowValidator().validate(2, listOf("2026-08-01", "Coffee Shop", "n/a"), mappingWithoutOptionalColumns)

        assertEquals(ImportRowErrorReason.INVALID_AMOUNT, (result as ImportRowResult.Invalid).error.reason)
    }

    @Test
    fun `amount with thousands separator is invalid`() {
        val result = ImportRowValidator().validate(2, listOf("2026-08-01", "Coffee Shop", "1,200.00"), mappingWithoutOptionalColumns)

        assertTrue(result is ImportRowResult.Invalid)
    }

    @Test
    fun `supports whole number, decimal, negative and explicit positive amounts`() {
        val cases = mapOf("1200" to 120000L, "12.5" to 1250L, "-4.50" to -450L, "+4.50" to 450L)

        cases.forEach { (amount, expectedMinorUnits) ->
            val result = ImportRowValidator().validate(
                2,
                listOf("2026-08-01", "Merchant", amount),
                mappingWithoutOptionalColumns,
            ) as ImportRowResult.Valid

            assertEquals(expectedMinorUnits, result.transaction.amount.minorUnits)
        }
    }

    @Test
    fun `first currency in the file becomes the expected currency`() {
        val validator = ImportRowValidator()

        val result = validator.validate(2, listOf("2026-08-01", "Merchant", "-4.50", "EUR"), mappingWithCurrency)

        assertTrue(result is ImportRowResult.Valid)
    }

    @Test
    fun `currency mismatch across rows in the same file is invalid`() {
        val validator = ImportRowValidator()
        validator.validate(2, listOf("2026-08-01", "Merchant", "-4.50", "EUR"), mappingWithCurrency)

        val result = validator.validate(3, listOf("2026-08-02", "Merchant", "-1.00", "USD"), mappingWithCurrency)

        assertEquals(ImportRowErrorReason.CURRENCY_MISMATCH, (result as ImportRowResult.Invalid).error.reason)
    }

    @Test
    fun `an earlier row rejected for a different reason still fixes the file's expected currency`() {
        val validator = ImportRowValidator()
        val invalidDateResult = validator.validate(2, listOf("bad-date", "A", "-1.00", "USD"), mappingWithCurrency)
        assertTrue(invalidDateResult is ImportRowResult.Invalid)

        val mismatch = validator.validate(3, listOf("2026-08-01", "B", "-1.00", "EUR"), mappingWithCurrency)
        val accepted = validator.validate(4, listOf("2026-08-02", "C", "-1.00", "USD"), mappingWithCurrency)

        assertEquals(ImportRowErrorReason.CURRENCY_MISMATCH, (mismatch as ImportRowResult.Invalid).error.reason)
        assertTrue(accepted is ImportRowResult.Valid)
    }

    @Test
    fun `blank currency is invalid once the column is declared`() {
        val result = ImportRowValidator().validate(2, listOf("2026-08-01", "Merchant", "-4.50", ""), mappingWithCurrency)

        assertEquals(ImportRowErrorReason.CURRENCY_MISMATCH, (result as ImportRowResult.Invalid).error.reason)
    }
}
