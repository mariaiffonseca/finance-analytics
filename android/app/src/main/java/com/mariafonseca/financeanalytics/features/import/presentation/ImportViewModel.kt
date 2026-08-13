package com.mariafonseca.financeanalytics.features.`import`.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mariafonseca.financeanalytics.features.`import`.data.CsvFileSource
import com.mariafonseca.financeanalytics.features.`import`.data.CsvFileSupport
import com.mariafonseca.financeanalytics.features.`import`.data.CsvImportPipeline
import com.mariafonseca.financeanalytics.features.`import`.data.CsvParseOutcome
import com.mariafonseca.financeanalytics.features.`import`.data.CsvRejectionReason
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.IOException

class ImportViewModel(
    private val csvFileSource: CsvFileSource,
    private val csvImportPipeline: CsvImportPipeline,
    // Injectable only so tests can pin file reading to a virtual-time test
    // dispatcher instead of the real IO thread pool; production always uses
    // the real Dispatchers.IO via the default.
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.SelectingFile)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun onFileSelected(uriString: String) {
        val fileName = csvFileSource.fileName(uriString)
        if (!CsvFileSupport.isSupported(fileName)) {
            _uiState.value = ImportUiState.Failed(ImportFailureReason.UnsupportedFileType)
            return
        }
        val resolvedName = fileName.orEmpty()

        viewModelScope.launch {
            // yield() after each transition, not just for the async steps: it
            // gives collectors (Compose recomposition, tests) a chance to
            // observe each state even though parsing/validation itself is
            // synchronous — without it, back-to-back `_uiState.value =` sets
            // would conflate and a fast phase could never be observed.
            _uiState.value = ImportUiState.Reading(resolvedName)
            yield()
            val csvText = try {
                withContext(ioDispatcher) { csvFileSource.readText(uriString) }
            } catch (e: IOException) {
                _uiState.value = ImportUiState.Failed(ImportFailureReason.FileReadError)
                return@launch
            }

            _uiState.value = ImportUiState.Validating(resolvedName)
            yield()
            when (val outcome = csvImportPipeline.parseAndValidate(csvText)) {
                is CsvParseOutcome.Rejected -> _uiState.value = ImportUiState.Failed(outcome.reason.toFailureReason())
                is CsvParseOutcome.Parsed -> {
                    _uiState.value = ImportUiState.Importing(resolvedName)
                    yield()
                    val result = csvImportPipeline.persist(outcome.parsedImport)
                    _uiState.value = ImportUiState.Completed(result)
                }
            }
        }
    }

    fun onRetry() {
        _uiState.value = ImportUiState.SelectingFile
    }

    private fun CsvRejectionReason.toFailureReason(): ImportFailureReason = when (this) {
        is CsvRejectionReason.EmptyFile -> ImportFailureReason.EmptyFile
        is CsvRejectionReason.MalformedCsv -> ImportFailureReason.MalformedCsv
        is CsvRejectionReason.MissingColumns -> ImportFailureReason.MissingColumns(missing)
    }
}
