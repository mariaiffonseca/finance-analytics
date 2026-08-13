package com.mariafonseca.financeanalytics.features.`import`.data

import com.mariafonseca.financeanalytics.features.transactions.data.TransactionRepository
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvImportPipelineTest {

    private val validCsv = """
        Date,Merchant,Amount,Category
        2026-08-01,Coffee Shop,-4.50,Food
        2026-08-02,Employer,1200.00,
        not-a-date,Broken Row,-1.00,
    """.trimIndent()

    @Test
    fun `parses and validates rows, separating valid from invalid`() {
        val pipeline = CsvImportPipeline(FakeTransactionRepository())

        val outcome = pipeline.parseAndValidate(validCsv) as CsvParseOutcome.Parsed

        assertEquals(3, outcome.parsedImport.rowsRead)
        assertEquals(2, outcome.parsedImport.validTransactions.size)
        assertEquals(1, outcome.parsedImport.invalidRows.size)
        assertEquals(4, outcome.parsedImport.invalidRows.first().rowNumber)
    }

    @Test
    fun `rejects an empty file`() {
        val pipeline = CsvImportPipeline(FakeTransactionRepository())

        val outcome = pipeline.parseAndValidate("")

        assertEquals(CsvParseOutcome.Rejected(CsvRejectionReason.EmptyFile), outcome)
    }

    @Test
    fun `rejects a file missing required columns`() {
        val pipeline = CsvImportPipeline(FakeTransactionRepository())

        val outcome = pipeline.parseAndValidate("Description,Value\nCoffee,-4.50\n") as CsvParseOutcome.Rejected

        assertTrue(outcome.reason is CsvRejectionReason.MissingColumns)
    }

    @Test
    fun `persist inserts valid transactions through the repository`() = runTest {
        val repository = FakeTransactionRepository()
        val pipeline = CsvImportPipeline(repository)
        val parsedImport = (pipeline.parseAndValidate(validCsv) as CsvParseOutcome.Parsed).parsedImport

        val result = pipeline.persist(parsedImport)

        assertEquals(2, result.importedCount)
        assertEquals(1, result.invalidRowCount)
        assertEquals(0, result.duplicateRowCount)
        assertEquals(2, repository.insertedTransactions.size)
    }

    @Test
    fun `duplicate rows within the same batch are counted once and imported once`() = runTest {
        val pipeline = CsvImportPipeline(FakeTransactionRepository())
        val csv = """
            Date,Merchant,Amount
            2026-08-01,Coffee Shop,-4.50
            2026-08-01,Coffee Shop,-4.50
        """.trimIndent()
        val parsedImport = (pipeline.parseAndValidate(csv) as CsvParseOutcome.Parsed).parsedImport

        val result = pipeline.persist(parsedImport)

        assertEquals(1, result.importedCount)
        assertEquals(1, result.duplicateRowCount)
    }

    @Test
    fun `re-importing the same file does not create duplicate transactions`() = runTest {
        val repository = FakeTransactionRepository()
        val pipeline = CsvImportPipeline(repository)
        val parsedImport = (pipeline.parseAndValidate(validCsv) as CsvParseOutcome.Parsed).parsedImport
        pipeline.persist(parsedImport)

        val secondParsedImport = (pipeline.parseAndValidate(validCsv) as CsvParseOutcome.Parsed).parsedImport
        val secondResult = pipeline.persist(secondParsedImport)

        assertEquals(0, secondResult.importedCount)
        assertEquals(2, secondResult.duplicateRowCount)
        assertEquals(2, repository.insertedTransactions.size)
    }
}

private class FakeTransactionRepository(
    initialTransactions: List<Transaction> = emptyList(),
) : TransactionRepository {

    private val transactionsFlow = MutableStateFlow(initialTransactions)
    val insertedTransactions: List<Transaction> get() = transactionsFlow.value

    override fun observeTransactions(): Flow<List<Transaction>> = transactionsFlow

    override suspend fun getTransaction(id: Long): Transaction? =
        transactionsFlow.value.firstOrNull { it.id == id }

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        transactionsFlow.value = transactionsFlow.value + transactions
    }

    override suspend fun deleteAllTransactions() {
        transactionsFlow.value = emptyList()
    }
}
