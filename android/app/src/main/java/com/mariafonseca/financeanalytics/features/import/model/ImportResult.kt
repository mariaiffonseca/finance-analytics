package com.mariafonseca.financeanalytics.features.`import`.model

import com.mariafonseca.financeanalytics.features.transactions.model.Transaction

/**
 * Explicit summary of one import run, suitable for driving the completed
 * state of the import UI (PR-006 section 5/9).
 */
data class ImportResult(
    val rowsRead: Int,
    val importedTransactions: List<Transaction>,
    val duplicateRowCount: Int,
    val invalidRows: List<ImportRowError>,
) {
    val importedCount: Int get() = importedTransactions.size
    val invalidRowCount: Int get() = invalidRows.size
}
