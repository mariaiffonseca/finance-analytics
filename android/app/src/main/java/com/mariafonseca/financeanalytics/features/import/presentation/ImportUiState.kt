package com.mariafonseca.financeanalytics.features.`import`.presentation

import com.mariafonseca.financeanalytics.features.`import`.model.ImportResult

/**
 * Presentation-level reason for [ImportUiState.Failed]. Kept separate from
 * the data-layer's `CsvRejectionReason`/`IOException` so the parsing and
 * pipeline code stays free of Android string-resource concerns (see
 * "Could parser/validation logic be reused outside Android?" in PR-006's
 * Engineering Reflection) — ImportViewModel maps into this, and
 * ImportScreen resolves it to copy.
 */
sealed interface ImportFailureReason {
    data object UnsupportedFileType : ImportFailureReason
    data object FileReadError : ImportFailureReason
    data object EmptyFile : ImportFailureReason
    data object MalformedCsv : ImportFailureReason
    data class MissingColumns(val missing: List<String>) : ImportFailureReason
    data object SaveError : ImportFailureReason
}

/** Select file -> Reading -> Validating -> Importing -> Completed/Failed (PR-006 section 9). */
sealed interface ImportUiState {
    data object SelectingFile : ImportUiState
    data class Reading(val fileName: String) : ImportUiState
    data class Validating(val fileName: String) : ImportUiState
    data class Importing(val fileName: String) : ImportUiState
    data class Completed(val result: ImportResult) : ImportUiState
    data class Failed(val reason: ImportFailureReason) : ImportUiState
}
