package com.mariafonseca.financeanalytics.features.`import`.data

/**
 * Whether a selected file is an accepted import source. Android's reported
 * MIME type for `.csv` files is inconsistent across file providers (some
 * report `text/csv`, others `text/comma-separated-values` or generic
 * `text/plain`), so the file extension — not the MIME type — is the
 * authoritative, deterministic check.
 *
 * A `null` name (some content providers don't populate
 * `OpenableColumns.DISPLAY_NAME`) is treated as supported rather than
 * rejected: we have no evidence it's *not* a CSV, and rejecting it here would
 * show a misleading "isn't a CSV" message with no recovery path. A
 * genuinely unsupported file still gets caught downstream, as a malformed
 * CSV or a missing-columns rejection.
 */
object CsvFileSupport {
    private const val CSV_EXTENSION = "csv"

    fun isSupported(fileName: String?): Boolean {
        if (fileName == null) return true
        return fileName.substringAfterLast('.', missingDelimiterValue = "").equals(CSV_EXTENSION, ignoreCase = true)
    }
}
