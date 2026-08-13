package com.mariafonseca.financeanalytics.features.`import`.data.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CsvParserTest {

    @Test
    fun `parses a simple valid csv`() {
        val content = "Date,Merchant,Amount\n2026-08-01,Coffee Shop,-4.50\n2026-08-02,Employer,1200.00\n"

        val rows = CsvParser.parse(content)

        assertEquals(
            listOf(
                listOf("Date", "Merchant", "Amount"),
                listOf("2026-08-01", "Coffee Shop", "-4.50"),
                listOf("2026-08-02", "Employer", "1200.00"),
            ),
            rows,
        )
    }

    @Test
    fun `handles quoted values containing commas and newlines`() {
        val content = "Date,Merchant,Amount\n2026-08-01,\"Coffee, Tea & Co.\",-4.50\n2026-08-02,\"Multi\nline\",-1.00\n"

        val rows = CsvParser.parse(content)

        assertEquals("Coffee, Tea & Co.", rows[1][1])
        assertEquals("Multi\nline", rows[2][1])
    }

    @Test
    fun `unescapes doubled quotes inside a quoted value`() {
        val content = "Merchant\n\"Say \"\"Hi\"\"\"\n"

        val rows = CsvParser.parse(content)

        assertEquals(listOf("Say \"Hi\""), rows[1])
    }

    @Test
    fun `preserves empty values`() {
        val content = "Date,Merchant,Amount,Category\n2026-08-01,Coffee Shop,-4.50,\n"

        val rows = CsvParser.parse(content)

        assertEquals(listOf("2026-08-01", "Coffee Shop", "-4.50", ""), rows[1])
    }

    @Test
    fun `skips blank lines`() {
        val content = "Date,Merchant,Amount\n2026-08-01,Coffee Shop,-4.50\n\n2026-08-02,Employer,1200.00\n"

        val rows = CsvParser.parse(content)

        assertEquals(3, rows.size)
    }

    @Test
    fun `a quote appearing mid-field is treated as a literal character, not a quote toggle`() {
        val content = "Date,Merchant,Amount\n2026-08-01,5'10\" Store,-4.50\n2026-08-02,Bob's \"Diner\",-1.00\n"

        val rows = CsvParser.parse(content)

        assertEquals(
            listOf(
                listOf("Date", "Merchant", "Amount"),
                listOf("2026-08-01", "5'10\" Store", "-4.50"),
                listOf("2026-08-02", "Bob's \"Diner\"", "-1.00"),
            ),
            rows,
        )
    }

    @Test
    fun `throws on an unterminated quoted value`() {
        val content = "Date,Merchant,Amount\n2026-08-01,\"Unterminated,-4.50\n"

        assertThrows(CsvParseException::class.java) { CsvParser.parse(content) }
    }
}
