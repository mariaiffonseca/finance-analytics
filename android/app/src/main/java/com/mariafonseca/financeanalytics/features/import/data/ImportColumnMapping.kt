package com.mariafonseca.financeanalytics.features.`import`.data

/**
 * Explicit CSV header -> transaction field mapping (PR-006 section 3). Header
 * names are matched case-insensitively but never fuzzily (e.g. "Desc" is
 * never treated as "Merchant") — anything else means the file is rejected as
 * an unsupported format rather than guessed at.
 *
 * Documented supported header names: `Date`, `Merchant`, `Amount` (required),
 * `Category` (optional), `Currency` (optional — see [ImportRowValidator] for
 * why it's validated but not persisted).
 */
data class ImportColumnMapping(
    val dateColumn: Int,
    val merchantColumn: Int,
    val amountColumn: Int,
    val categoryColumn: Int?,
    val currencyColumn: Int?,
) {
    companion object {
        private const val DATE_HEADER = "date"
        private const val MERCHANT_HEADER = "merchant"
        private const val AMOUNT_HEADER = "amount"
        private const val CATEGORY_HEADER = "category"
        private const val CURRENCY_HEADER = "currency"
        private val REQUIRED_HEADERS = listOf(DATE_HEADER, MERCHANT_HEADER, AMOUNT_HEADER)

        fun from(header: List<String>): ImportColumnMappingResult {
            val normalized = header.map { it.trim().lowercase() }
            val missing = REQUIRED_HEADERS.filter { it !in normalized }
            if (missing.isNotEmpty()) {
                return ImportColumnMappingResult.MissingColumns(missing)
            }
            return ImportColumnMappingResult.Found(
                ImportColumnMapping(
                    dateColumn = normalized.indexOf(DATE_HEADER),
                    merchantColumn = normalized.indexOf(MERCHANT_HEADER),
                    amountColumn = normalized.indexOf(AMOUNT_HEADER),
                    categoryColumn = normalized.indexOf(CATEGORY_HEADER).takeIf { it >= 0 },
                    currencyColumn = normalized.indexOf(CURRENCY_HEADER).takeIf { it >= 0 },
                ),
            )
        }
    }
}

sealed interface ImportColumnMappingResult {
    data class Found(val mapping: ImportColumnMapping) : ImportColumnMappingResult
    data class MissingColumns(val missing: List<String>) : ImportColumnMappingResult
}
