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
 * fields.
 *
 * A leading UTF-8 BOM is stripped (common in "CSV UTF-8" exports from
 * Excel). `\r\n`, lone `\r` (old Mac-style) and lone `\n` all end a row.
 *
 * Interior blank lines are preserved as a row of blank values rather than
 * silently dropped, so a row's position here — and the 1-based row number
 * the caller derives from it — always matches the line a user would see if
 * they opened the file in a spreadsheet; only a single trailing blank line
 * is trimmed, since bank exports commonly end with one and it has no
 * corresponding line a user would count.
 */
object CsvParser {

    private const val DELIMITER = ','
    private const val QUOTE = '"'
    private const val BYTE_ORDER_MARK_CODE_POINT = 0xFEFF

    fun parse(content: String): List<List<String>> {
        val text = if (content.isNotEmpty() && content[0].code == BYTE_ORDER_MARK_CODE_POINT) {
            content.substring(1)
        } else {
            content
        }
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        // A `"` only opens quoted mode at the very start of a field — per
        // RFC 4180, a quote elsewhere (e.g. `5'10" Store`) is just a literal
        // character, not a quote-state toggle.
        var atFieldStart = true
        var index = 0

        fun endField() {
            currentRow.add(field.toString())
            field.clear()
            atFieldStart = true
        }

        fun endRow() {
            endField()
            rows.add(currentRow)
            currentRow = mutableListOf()
        }

        while (index < text.length) {
            val char = text[index]
            when {
                inQuotes && char == QUOTE && index + 1 < text.length && text[index + 1] == QUOTE -> {
                    field.append(QUOTE)
                    index++
                }
                inQuotes && char == QUOTE -> inQuotes = false
                inQuotes -> field.append(char)
                char == QUOTE && atFieldStart -> {
                    inQuotes = true
                    atFieldStart = false
                }
                char == DELIMITER -> endField()
                char == '\r' -> if (index + 1 >= text.length || text[index + 1] != '\n') endRow()
                char == '\n' -> endRow()
                else -> {
                    field.append(char)
                    atFieldStart = false
                }
            }
            index++
        }

        if (inQuotes) {
            throw CsvParseException("Unterminated quoted value")
        }
        if (field.isNotEmpty() || currentRow.isNotEmpty()) {
            endRow()
        }
        if (rows.isNotEmpty() && rows.last().all { it.isBlank() }) {
            rows.removeAt(rows.lastIndex)
        }
        return rows
    }
}
