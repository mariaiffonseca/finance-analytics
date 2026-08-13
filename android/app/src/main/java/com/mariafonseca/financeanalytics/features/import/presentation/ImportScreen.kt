package com.mariafonseca.financeanalytics.features.`import`.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mariafonseca.financeanalytics.R
import com.mariafonseca.financeanalytics.core.designsystem.LocalFinanceColors
import com.mariafonseca.financeanalytics.core.designsystem.Space16
import com.mariafonseca.financeanalytics.core.designsystem.Space24
import com.mariafonseca.financeanalytics.core.designsystem.Space8
import com.mariafonseca.financeanalytics.features.`import`.model.ImportResult
import org.koin.androidx.compose.koinViewModel

private val SquareShape = RoundedCornerShape(0.dp)

@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    viewModel: ImportViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onFileSelected(it.toString()) }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Space24)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Space16, Alignment.CenterVertically),
        ) {
            when (val state = uiState) {
                ImportUiState.SelectingFile -> SelectFileContent(
                    // "*/*" rather than a CSV mime type: providers report .csv
                    // inconsistently (see CsvFileSupport), so the extension
                    // check after selection is the real gate, not the picker filter.
                    onChooseFile = { pickFile.launch("*/*") },
                    onBack = onBack,
                )
                is ImportUiState.Reading -> ProgressContent(R.string.import_reading, state.fileName, onBack)
                is ImportUiState.Validating -> ProgressContent(R.string.import_validating, state.fileName, onBack)
                is ImportUiState.Importing -> ProgressContent(R.string.import_importing, state.fileName, onBack)
                is ImportUiState.Completed -> CompletedContent(
                    result = state.result,
                    onContinue = onContinue,
                    onImportAnotherFile = viewModel::onRetry,
                )
                is ImportUiState.Failed -> FailedContent(
                    reason = state.reason,
                    onRetry = viewModel::onRetry,
                    onBack = onBack,
                    onSkip = onSkip,
                )
            }
        }
    }
}

@Composable
private fun SelectFileContent(onChooseFile: () -> Unit, onBack: () -> Unit) {
    val colors = LocalFinanceColors.current
    Text(text = stringResource(R.string.import_title), style = MaterialTheme.typography.headlineSmall)
    Text(text = stringResource(R.string.import_body), style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
    Button(onClick = onChooseFile, modifier = Modifier.fillMaxWidth(), shape = SquareShape) {
        Text(text = stringResource(R.string.action_choose_csv_file), fontWeight = FontWeight.Bold)
    }
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = SquareShape) {
        Text(text = stringResource(R.string.action_back))
    }
}

@Composable
private fun ProgressContent(@StringRes labelRes: Int, fileName: String, onBack: () -> Unit) {
    val colors = LocalFinanceColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space16, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator(color = colors.accent)
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.titleLarge)
        Text(
            // Blank when the content provider never populated
            // OpenableColumns.DISPLAY_NAME — CsvFileSupport intentionally
            // still allows the import to proceed, so this needs its own
            // fallback rather than showing a blank line.
            text = fileName.ifBlank { stringResource(R.string.import_unnamed_file) },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = SquareShape) {
            Text(text = stringResource(R.string.action_back))
        }
    }
}

@Composable
private fun CompletedContent(
    result: ImportResult,
    onContinue: () -> Unit,
    onImportAnotherFile: () -> Unit,
) {
    val colors = LocalFinanceColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(color = colors.positiveTint, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✓", style = MaterialTheme.typography.titleLarge, color = colors.positiveDeep)
        }
    }

    Text(
        text = stringResource(R.string.import_completed_title),
        style = MaterialTheme.typography.headlineSmall,
    )

    Column(verticalArrangement = Arrangement.spacedBy(Space8)) {
        SummaryRow(R.string.import_summary_rows_processed, result.rowsRead.toString())
        SummaryRow(R.string.import_summary_imported, result.importedCount.toString())
        SummaryRow(R.string.import_summary_duplicates, result.duplicateRowCount.toString())
        SummaryRow(R.string.import_summary_invalid, result.invalidRowCount.toString())
    }

    if (result.invalidRows.isNotEmpty()) {
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(Space8)) {
            result.invalidRows.take(MAX_VISIBLE_ROW_ERRORS).forEach { error ->
                Text(
                    text = stringResource(R.string.import_row_error, error.rowNumber, error.message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            val remaining = result.invalidRows.size - MAX_VISIBLE_ROW_ERRORS
            if (remaining > 0) {
                Text(
                    text = stringResource(R.string.import_row_error_more, remaining),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }
    }

    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), shape = SquareShape) {
        Text(text = stringResource(R.string.action_continue), fontWeight = FontWeight.Bold)
    }
    OutlinedButton(onClick = onImportAnotherFile, modifier = Modifier.fillMaxWidth(), shape = SquareShape) {
        Text(text = stringResource(R.string.action_import_another_file))
    }
}

@Composable
private fun SummaryRow(@StringRes labelRes: Int, value: String) {
    val colors = LocalFinanceColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
        Text(text = value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun FailedContent(reason: ImportFailureReason, onRetry: () -> Unit, onBack: () -> Unit, onSkip: () -> Unit) {
    val colors = LocalFinanceColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colors.errorTint)
            .padding(Space16),
        verticalArrangement = Arrangement.spacedBy(Space8),
    ) {
        Text(
            text = stringResource(R.string.import_failed_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.accentDeep,
        )
        Text(
            text = failureMessage(reason),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text,
        )
    }

    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth(), shape = SquareShape) {
        Text(text = stringResource(R.string.action_try_again), fontWeight = FontWeight.Bold)
    }
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = SquareShape) {
        Text(text = stringResource(R.string.action_back))
    }
    // Escape hatch for a file that will never validate (wrong headers,
    // unsupported encoding, ...): without this, Back only reaches
    // EmptyScreen, whose sole action re-enters this same Failed loop.
    OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth(), shape = SquareShape) {
        Text(text = stringResource(R.string.action_skip_import))
    }
}

@Composable
private fun failureMessage(reason: ImportFailureReason): String = when (reason) {
    ImportFailureReason.UnsupportedFileType -> stringResource(R.string.import_error_unsupported_file_type)
    ImportFailureReason.FileReadError -> stringResource(R.string.import_error_file_read)
    ImportFailureReason.EmptyFile -> stringResource(R.string.import_error_empty_file)
    ImportFailureReason.MalformedCsv -> stringResource(R.string.import_error_malformed_csv)
    is ImportFailureReason.MissingColumns -> stringResource(
        R.string.import_error_missing_columns,
        reason.missing.joinToString(", "),
    )
    ImportFailureReason.SaveError -> stringResource(R.string.import_error_save)
}

private const val MAX_VISIBLE_ROW_ERRORS = 5
