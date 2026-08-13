package com.mariafonseca.financeanalytics.features.`import`.data

import com.mariafonseca.financeanalytics.features.`import`.data.csv.CsvParseException
import com.mariafonseca.financeanalytics.features.`import`.data.csv.CsvParser
import com.mariafonseca.financeanalytics.features.`import`.model.ImportResult
import com.mariafonseca.financeanalytics.features.`import`.model.ImportRowError
import com.mariafonseca.financeanalytics.features.transactions.data.TransactionRepository
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/** Why an entire file was rejected, before any row-level validation could run. */
sealed interface CsvRejectionReason {
    data object EmptyFile : CsvRejectionReason
    data object MalformedCsv : CsvRejectionReason
    data class MissingColumns(val missing: List<String>) : CsvRejectionReason
}

sealed interface CsvParseOutcome {
    data class Parsed(val parsedImport: ParsedImport) : CsvParseOutcome
    data class Rejected(val reason: CsvRejectionReason) : CsvParseOutcome
}

/** Parsed + validated rows, not yet deduplicated or persisted. */
data class ParsedImport(
    val rowsRead: Int,
    val validTransactions: List<Transaction>,
    val invalidRows: List<ImportRowError>,
)

/**
 * CSV file -> parser -> validation -> transaction mapping -> [TransactionRepository]
 * (PR-006's ingestion pipeline). Split into two steps so the caller (the
 * import ViewModel) can reflect "Validating" vs "Importing" as distinct UI
 * states around a real boundary rather than a fake delay.
 *
 * Never touches Room directly — [persist] only goes through
 * [TransactionRepository], same as every other feature.
 */
class CsvImportPipeline(
    private val transactionRepository: TransactionRepository,
) {

    // Guards the read-check-then-write in persist(): without it, two
    // overlapping calls on this same (Koin-singleton) instance could both
    // read the same existingKeys snapshot before either insert lands,
    // letting the exact duplicates dedup is meant to prevent through.
    private val persistMutex = Mutex()

    fun parseAndValidate(csvText: String): CsvParseOutcome {
        val rows = try {
            CsvParser.parse(csvText)
        } catch (e: CsvParseException) {
            return CsvParseOutcome.Rejected(CsvRejectionReason.MalformedCsv)
        }
        if (rows.isEmpty()) {
            return CsvParseOutcome.Rejected(CsvRejectionReason.EmptyFile)
        }

        val mappingResult = ImportColumnMapping.from(rows.first())
        val mapping = when (mappingResult) {
            is ImportColumnMappingResult.MissingColumns ->
                return CsvParseOutcome.Rejected(CsvRejectionReason.MissingColumns(mappingResult.missing))
            is ImportColumnMappingResult.Found -> mappingResult.mapping
        }

        val dataRows = rows.drop(1)
        val validator = ImportRowValidator()
        val validTransactions = mutableListOf<Transaction>()
        val invalidRows = mutableListOf<ImportRowError>()
        var rowsRead = 0

        // rowNumber comes from `index` into the *unfiltered* dataRows list
        // (CsvParser now preserves interior blank lines instead of dropping
        // them), so it always matches the line a user would see if they
        // opened the file in a spreadsheet — a blank line is skipped here,
        // not upstream, precisely so it doesn't shift every later row number.
        dataRows.forEachIndexed { index, values ->
            if (values.all { it.isBlank() }) return@forEachIndexed
            rowsRead++
            val rowNumber = index + 2 // +1 for 1-based numbering, +1 to account for the header row
            when (val result = validator.validate(rowNumber, values, mapping)) {
                is ImportRowResult.Valid -> validTransactions += result.transaction
                is ImportRowResult.Invalid -> invalidRows += result.error
            }
        }

        return CsvParseOutcome.Parsed(
            ParsedImport(
                rowsRead = rowsRead,
                validTransactions = validTransactions,
                invalidRows = invalidRows,
            ),
        )
    }

    /**
     * Deduplicates against both already-persisted transactions and the rest
     * of this same batch, then inserts the remainder (PR-006 section 8).
     *
     * Duplicate key: (date, merchant, amount). Bank CSV exports rarely carry
     * a stable external transaction id, so an auto-generated Room id can't
     * be relied on — this composite key is the deterministic alternative.
     * Known limitation, documented rather than silently accepted: two
     * genuinely distinct transactions with the same date, merchant and
     * amount on the same day are indistinguishable and will be treated as a
     * duplicate.
     */
    suspend fun persist(parsedImport: ParsedImport): ImportResult = persistMutex.withLock {
        // Scoped to the batch's own date span rather than the whole table:
        // cost grows with the file being imported, not with lifetime
        // transaction count. TransactionEntity also carries a unique index
        // on this same key as a DB-level backstop in case this app-level
        // check is ever bypassed.
        val batchDates = parsedImport.validTransactions.map { it.date }
        val existingKeys = if (batchDates.isEmpty()) {
            emptySet()
        } else {
            transactionRepository.observeTransactions(batchDates.min(), batchDates.max()).first()
                .map { it.duplicateKey() }
                .toSet()
        }
        val seenKeys = mutableSetOf<DuplicateKey>()
        val toInsert = mutableListOf<Transaction>()
        var duplicateCount = 0

        for (transaction in parsedImport.validTransactions) {
            val key = transaction.duplicateKey()
            if (key in existingKeys || !seenKeys.add(key)) {
                duplicateCount++
            } else {
                toInsert += transaction
            }
        }

        if (toInsert.isNotEmpty()) {
            transactionRepository.insertTransactions(toInsert)
        }

        ImportResult(
            rowsRead = parsedImport.rowsRead,
            importedTransactions = toInsert,
            duplicateRowCount = duplicateCount,
            invalidRows = parsedImport.invalidRows,
        )
    }

    private data class DuplicateKey(val date: LocalDate, val merchant: String, val amountMinorUnits: Long)

    // Case-insensitive: the same merchant can arrive with different
    // capitalization across exports (e.g. "STARBUCKS #123" vs
    // "Starbucks #123"), and TransactionEntity's unique index matches this
    // via NOCASE collation on the merchant column.
    private fun Transaction.duplicateKey() = DuplicateKey(date, merchant.lowercase(), amount.minorUnits)
}
