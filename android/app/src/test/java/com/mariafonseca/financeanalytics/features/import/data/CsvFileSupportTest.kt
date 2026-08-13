package com.mariafonseca.financeanalytics.features.`import`.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvFileSupportTest {

    @Test
    fun `accepts csv extension regardless of case`() {
        assertTrue(CsvFileSupport.isSupported("statement.csv"))
        assertTrue(CsvFileSupport.isSupported("STATEMENT.CSV"))
    }

    @Test
    fun `rejects other extensions`() {
        assertFalse(CsvFileSupport.isSupported("statement.pdf"))
        assertFalse(CsvFileSupport.isSupported("statement.xlsx"))
    }

    @Test
    fun `rejects a file with no extension`() {
        assertFalse(CsvFileSupport.isSupported("statement"))
    }

    @Test
    fun `treats an undeterminable name as supported rather than rejecting it`() {
        assertTrue(CsvFileSupport.isSupported(null))
    }
}
