package com.mariafonseca.financeanalytics.features.`import`.data

/**
 * Whether a selected file is an accepted import source. Android's reported
 * MIME type for `.csv` files is inconsistent across file providers (some
 * report `text/csv`, others `text/comma-separated-values` or generic
 * `text/plain`), so the file extension — not the MIME type — is the
 * authoritative, deterministic check.
 */
object CsvFileSupport {
    private const val CSV_EXTENSION = "csv"

    fun isSupported(fileName: String?): Boolean =
        fileName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.equals(CSV_EXTENSION, ignoreCase = true) == true
}
