package com.mariafonseca.financeanalytics.features.`import`.presentation

import android.database.SQLException
import com.mariafonseca.financeanalytics.core.testing.FakeTransactionRepository
import com.mariafonseca.financeanalytics.features.`import`.data.CsvFileSource
import com.mariafonseca.financeanalytics.features.`import`.data.CsvImportPipeline
import com.mariafonseca.financeanalytics.features.transactions.data.TransactionRepository
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

private const val SOME_URI = "content://fake/statement.csv"

@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is selecting file`() {
        val viewModel = buildViewModel()

        assertTrue(viewModel.uiState.value is ImportUiState.SelectingFile)
    }

    @Test
    fun `unsupported file type fails without reading the file`() {
        val fileSource = FakeCsvFileSource(name = "statement.pdf", text = "irrelevant")
        val viewModel = buildViewModel(fileSource = fileSource)

        viewModel.onFileSelected(SOME_URI)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ImportUiState.Failed(ImportFailureReason.UnsupportedFileType), viewModel.uiState.value)
        assertEquals(0, fileSource.readCount)
    }

    @Test
    fun `a valid csv file moves through reading, validating, importing to completed`() = runTest(dispatcher) {
        // Must share `dispatcher`'s scheduler with Main/io (see buildViewModel):
        // runTest's own default scheduler would otherwise govern backgroundScope
        // while a *different* scheduler drives the ViewModel's coroutine, so
        // advancing one would never advance the other.
        val csv = "Date,Merchant,Amount\n2026-08-01,Coffee Shop,-4.50\n"
        val fileSource = FakeCsvFileSource(name = "statement.csv", text = csv)
        val viewModel = buildViewModel(fileSource = fileSource)
        val states = mutableListOf<ImportUiState>()
        val collector = backgroundScope.launch { viewModel.uiState.toList(states) }

        viewModel.onFileSelected(SOME_URI)
        dispatcher.scheduler.advanceUntilIdle()
        collector.cancel()

        // The collected history proves the state machine visits Reading,
        // Validating and Importing in order before settling; the final value
        // is asserted directly off uiState.value since a StateFlow collector
        // isn't guaranteed to observe the very last emission before its
        // producer coroutine completes in the same dispatcher drain.
        assertEquals(
            listOf(
                ImportUiState.SelectingFile::class,
                ImportUiState.Reading::class,
                ImportUiState.Validating::class,
                ImportUiState.Importing::class,
            ),
            states.map { it::class },
        )
        val completed = viewModel.uiState.value as ImportUiState.Completed
        assertEquals(1, completed.result.importedCount)
    }

    @Test
    fun `a file that fails to read becomes a file read error`() {
        val fileSource = FakeCsvFileSource(name = "statement.csv", text = null)
        val viewModel = buildViewModel(fileSource = fileSource)

        viewModel.onFileSelected(SOME_URI)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ImportUiState.Failed(ImportFailureReason.FileReadError), viewModel.uiState.value)
    }

    @Test
    fun `a revoked read grant while resolving the file name becomes a file read error`() {
        val fileSource = FakeCsvFileSource(name = "statement.csv", text = "irrelevant", failNameWith = SecurityException())
        val viewModel = buildViewModel(fileSource = fileSource)

        viewModel.onFileSelected(SOME_URI)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ImportUiState.Failed(ImportFailureReason.FileReadError), viewModel.uiState.value)
    }

    @Test
    fun `a persistence failure becomes a save error`() = runTest(dispatcher) {
        val csv = "Date,Merchant,Amount\n2026-08-01,Coffee Shop,-4.50\n"
        val fileSource = FakeCsvFileSource(name = "statement.csv", text = csv)
        val viewModel = ImportViewModel(
            csvFileSource = fileSource,
            csvImportPipeline = CsvImportPipeline(ThrowingTransactionRepository()),
            ioDispatcher = dispatcher,
        )

        viewModel.onFileSelected(SOME_URI)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ImportUiState.Failed(ImportFailureReason.SaveError), viewModel.uiState.value)
    }

    @Test
    fun `a csv missing required columns fails with the missing columns`() {
        val fileSource = FakeCsvFileSource(name = "statement.csv", text = "Description,Value\nCoffee,-4.50\n")
        val viewModel = buildViewModel(fileSource = fileSource)

        viewModel.onFileSelected(SOME_URI)
        dispatcher.scheduler.advanceUntilIdle()

        val failed = viewModel.uiState.value as ImportUiState.Failed
        assertTrue(failed.reason is ImportFailureReason.MissingColumns)
    }

    @Test
    fun `retry resets state to selecting file`() {
        val fileSource = FakeCsvFileSource(name = "statement.pdf", text = "irrelevant")
        val viewModel = buildViewModel(fileSource = fileSource)
        viewModel.onFileSelected(SOME_URI)

        viewModel.onRetry()

        assertTrue(viewModel.uiState.value is ImportUiState.SelectingFile)
    }

    private fun buildViewModel(
        fileSource: CsvFileSource = FakeCsvFileSource(name = "statement.csv", text = "Date,Merchant,Amount\n"),
    ): ImportViewModel = ImportViewModel(
        csvFileSource = fileSource,
        csvImportPipeline = CsvImportPipeline(FakeTransactionRepository()),
        ioDispatcher = dispatcher,
    )
}

private class FakeCsvFileSource(
    private val name: String?,
    private val text: String?,
    private val failNameWith: Exception? = null,
) : CsvFileSource {

    var readCount = 0
        private set

    override fun fileName(uriString: String): String? {
        failNameWith?.let { throw it }
        return name
    }

    override fun readText(uriString: String): String {
        readCount++
        return text ?: throw IOException("Unable to open the selected file")
    }
}

private class ThrowingTransactionRepository : TransactionRepository {

    override fun observeTransactions(): Flow<List<Transaction>> = MutableStateFlow(emptyList())

    override suspend fun getTransaction(id: Long): Transaction? = null

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        throw SQLException("disk I/O error")
    }

    override suspend fun deleteAllTransactions() = Unit
}
