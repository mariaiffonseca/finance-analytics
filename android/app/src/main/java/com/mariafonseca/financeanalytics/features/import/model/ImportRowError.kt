package com.mariafonseca.financeanalytics.features.`import`.model

/**
 * Why a single CSV row was rejected. Kept as a closed set (rather than a
 * free-form string) so the presentation layer can localise/style each case
 * without parsing message text.
 */
enum class ImportRowErrorReason {
    MISSING_DATE,
    MISSING_MERCHANT,
    MISSING_AMOUNT,
    INVALID_DATE,
    INVALID_AMOUNT,
    CURRENCY_MISMATCH,
}

/**
 * A structured validation failure for one CSV row. [rowNumber] is 1-based
 * and counts the header row, so the first data row is row 2 — matching what
 * a user sees if they open the file in a spreadsheet.
 */
data class ImportRowError(
    val rowNumber: Int,
    val reason: ImportRowErrorReason,
    val message: String,
)
