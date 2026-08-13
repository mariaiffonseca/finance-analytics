package com.mariafonseca.financeanalytics.features.`import`.data.csv

/**
 * Thrown when the raw CSV text cannot be tokenised at all (e.g. an
 * unterminated quoted value). Row-level problems (missing values, bad dates,
 * wrong column count, ...) are never fatal here — they surface later as
 * structured [com.mariafonseca.financeanalytics.features.`import`.model.ImportRowError]s
 * so a few bad rows never block the rest of the file.
 */
class CsvParseException(message: String) : Exception(message)

/**
 * Minimal RFC 4180-style tokeniser: comma-delimited, double-quoted fields
 * (commas/newlines allowed inside quotes, `""` is an escaped quote). This is
 * intentionally not a general-purpose CSV library — see
 * docs/execution/02_DATA_FOUNDATION/PR-006_CSV_IMPORT_INGESTION.md ("Do not
 * build a universal CSV parser"). Header detection and column mapping are
 * handled by the caller; this only turns text into rows of raw string
 * fields. Blank lines are skipped since bank exports commonly end with one.
 */
object CsvParser {

    private const val DELIMITER = ','
    private const val QUOTE = '"'

    fun parse(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0

        fun endField() {
            currentRow.add(field.toString())
            field.clear()
        }

        fun endRow() {
            endField()
            if (currentRow.size > 1 || currentRow[0].isNotBlank()) {
                rows.add(currentRow)
            }
            currentRow = mutableListOf()
        }

        while (index < content.length) {
            val char = content[index]
            when {
                inQuotes && char == QUOTE && index + 1 < content.length && content[index + 1] == QUOTE -> {
                    field.append(QUOTE)
                    index++
                }
                inQuotes && char == QUOTE -> inQuotes = false
                inQuotes -> field.append(char)
                char == QUOTE -> inQuotes = true
                char == DELIMITER -> endField()
                char == '\r' -> Unit
                char == '\n' -> endRow()
                else -> field.append(char)
            }
            index++
        }

        if (inQuotes) {
            throw CsvParseException("Unterminated quoted value")
        }
        if (field.isNotEmpty() || currentRow.isNotEmpty()) {
            endRow()
        }
        return rows
    }
}
