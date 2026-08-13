package com.mariafonseca.financeanalytics.features.`import`.presentation

import android.database.SQLException
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
        viewModelScope.launch {
            // fileName() can be a blocking ContentResolver query (a network
            // round-trip for cloud-backed providers like Drive/Dropbox) and
            // can throw SecurityException/IllegalArgumentException for a
            // revoked or malformed URI, so it's offloaded and guarded just
            // like readText() below rather than run inline on the caller's
            // thread.
            val fileName = try {
                withContext(ioDispatcher) { csvFileSource.fileName(uriString) }
            } catch (e: SecurityException) {
                _uiState.value = ImportUiState.Failed(ImportFailureReason.FileReadError)
                return@launch
            } catch (e: IllegalArgumentException) {
                _uiState.value = ImportUiState.Failed(ImportFailureReason.FileReadError)
                return@launch
            }
            if (!CsvFileSupport.isSupported(fileName)) {
                _uiState.value = ImportUiState.Failed(ImportFailureReason.UnsupportedFileType)
                return@launch
            }
            val resolvedName = fileName.orEmpty()

            _uiState.value = ImportUiState.Reading(resolvedName)
            val csvText = try {
                withContext(ioDispatcher) { csvFileSource.readText(uriString) }
            } catch (e: IOException) {
                _uiState.value = ImportUiState.Failed(ImportFailureReason.FileReadError)
                return@launch
            } catch (e: SecurityException) {
                _uiState.value = ImportUiState.Failed(ImportFailureReason.FileReadError)
                return@launch
            }

            _uiState.value = ImportUiState.Validating(resolvedName)
            // Parsing/validation is CPU-bound (regex + BigDecimal + LocalDate
            // parsing per row), so it's offloaded the same way readText() is
            // rather than run inline on the Main dispatcher.
            when (val outcome = withContext(ioDispatcher) { csvImportPipeline.parseAndValidate(csvText) }) {
                is CsvParseOutcome.Rejected -> _uiState.value = ImportUiState.Failed(outcome.reason.toFailureReason())
                is CsvParseOutcome.Parsed -> {
                    _uiState.value = ImportUiState.Importing(resolvedName)
                    val result = try {
                        withContext(ioDispatcher) { csvImportPipeline.persist(outcome.parsedImport) }
                    } catch (e: SQLException) {
                        _uiState.value = ImportUiState.Failed(ImportFailureReason.SaveError)
                        return@launch
                    }
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
