package com.mariafonseca.financeanalytics.features.`import`.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportColumnMappingTest {

    @Test
    fun `maps required and optional columns case-insensitively`() {
        val result = ImportColumnMapping.from(listOf("date", "Merchant", "AMOUNT", "Category", "Currency"))

        val mapping = (result as ImportColumnMappingResult.Found).mapping
        assertEquals(0, mapping.dateColumn)
        assertEquals(1, mapping.merchantColumn)
        assertEquals(2, mapping.amountColumn)
        assertEquals(3, mapping.categoryColumn)
        assertEquals(4, mapping.currencyColumn)
    }

    @Test
    fun `optional columns are null when absent`() {
        val result = ImportColumnMapping.from(listOf("Date", "Merchant", "Amount"))

        val mapping = (result as ImportColumnMappingResult.Found).mapping
        assertEquals(null, mapping.categoryColumn)
        assertEquals(null, mapping.currencyColumn)
    }

    @Test
    fun `reports every missing required column`() {
        val result = ImportColumnMapping.from(listOf("Merchant"))

        val missing = (result as ImportColumnMappingResult.MissingColumns).missing
        assertTrue("date" in missing)
        assertTrue("amount" in missing)
        assertTrue("merchant" !in missing)
    }

    @Test
    fun `does not fuzzily match unrelated header names`() {
        val result = ImportColumnMapping.from(listOf("Description", "Value"))

        assertTrue(result is ImportColumnMappingResult.MissingColumns)
    }
}
