package com.mariafonseca.financeanalytics.features.transactions.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mariafonseca.financeanalytics.R
import com.mariafonseca.financeanalytics.core.common.formatMajorUnits
import com.mariafonseca.financeanalytics.core.common.toMajorUnits
import com.mariafonseca.financeanalytics.core.designsystem.LocalFinanceColors
import com.mariafonseca.financeanalytics.core.designsystem.Space12
import com.mariafonseca.financeanalytics.core.designsystem.Space24
import com.mariafonseca.financeanalytics.core.designsystem.Space8
import com.mariafonseca.financeanalytics.core.designsystem.component.FinanceFilterChip
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionsContent(
        uiState = uiState,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onTransactionSelected = viewModel::onTransactionSelected,
        onTransactionDetailDismissed = viewModel::onTransactionDetailDismissed,
    )
}

/** State-driven body, free of ViewModel/DI dependencies — same convention as InsightsContent/OverviewContent. */
@Composable
fun TransactionsContent(
    uiState: TransactionsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onTransactionSelected: (Transaction) -> Unit,
    onTransactionDetailDismissed: () -> Unit,
) {
    val colors = LocalFinanceColors.current

    Scaffold { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.padding(horizontal = Space24, vertical = Space12)) {
                Text(text = stringResource(uiState.titleRes), style = MaterialTheme.typography.headlineSmall)

                if (uiState.transactions.isEmpty()) {
                    Spacer(modifier = Modifier.height(Space24))
                    Text(
                        text = stringResource(uiState.placeholderMessageRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textSecondary,
                    )
                } else {
                    Spacer(modifier = Modifier.height(Space12))
                    SearchField(query = uiState.searchQuery, onQueryChanged = onSearchQueryChanged)
                    if (uiState.categories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Space12))
                        CategoryFilterRow(
                            categories = uiState.categories,
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = onCategorySelected,
                        )
                    }
                }
            }

            if (uiState.transactions.isNotEmpty()) {
                val filtered = uiState.filteredTransactions
                if (filtered.isEmpty()) {
                    Text(
                        text = stringResource(R.string.transactions_no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = Space24, vertical = Space12),
                    )
                } else {
                    TransactionList(transactions = filtered, onTransactionSelected = onTransactionSelected)
                }
            }
        }
    }

    if (uiState.selectedTransaction != null) {
        TransactionDetailSheet(transaction = uiState.selectedTransaction, onDismiss = onTransactionDetailDismissed)
    }
}

@Composable
private fun SearchField(query: String, onQueryChanged: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = stringResource(R.string.transactions_search_placeholder)) },
        singleLine = true,
        shape = RoundedCornerShape(0.dp),
    )
}

@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Space8), modifier = Modifier.selectableGroup()) {
        item {
            FinanceFilterChip(
                label = stringResource(R.string.transaction_filter_all),
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
            )
        }
        items(categories) { category ->
            FinanceFilterChip(
                label = category,
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
            )
        }
    }
}

@Composable
private fun TransactionList(transactions: List<Transaction>, onTransactionSelected: (Transaction) -> Unit) {
    val colors = LocalFinanceColors.current
    val groups = remember(transactions) { transactions.groupedByDateDescending() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()) }

    LazyColumn(contentPadding = PaddingValues(horizontal = Space24, vertical = Space12)) {
        groups.forEach { (date, dayTransactions) ->
            item(key = "header-$date") {
                Text(
                    text = date.format(dateFormatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = Space8),
                )
            }
            items(dayTransactions, key = { it.id }) { transaction ->
                TransactionRow(transaction = transaction, onClick = { onTransactionSelected(transaction) })
                HorizontalDivider(color = colors.divider)
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, onClick: () -> Unit) {
    val colors = LocalFinanceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Space12),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(text = transaction.merchant, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            transaction.category?.takeIf(String::isNotBlank)?.let { category ->
                Text(text = category, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
        }
        Text(
            text = signedAmountText(transaction),
            style = MaterialTheme.typography.bodyLarge,
            color = if (transaction.amount.minorUnits >= 0) colors.positive else colors.text,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailSheet(transaction: Transaction, onDismiss: () -> Unit) {
    val colors = LocalFinanceColors.current
    val sheetState = rememberModalBottomSheetState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = colors.surfaceElevated) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Space24, vertical = Space12)) {
            Text(text = transaction.merchant, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(Space8))
            Text(
                text = signedAmountText(transaction),
                style = MaterialTheme.typography.displayLarge,
                color = if (transaction.amount.minorUnits >= 0) colors.positive else colors.text,
            )
            Spacer(modifier = Modifier.height(Space24))
            HorizontalDivider(color = colors.divider)
            Spacer(modifier = Modifier.height(Space12))
            DetailRow(labelRes = R.string.transaction_detail_date_label, value = transaction.date.format(dateFormatter))
            DetailRow(
                labelRes = R.string.transaction_detail_category_label,
                value = transaction.category?.takeIf(String::isNotBlank)
                    ?: stringResource(R.string.transaction_detail_uncategorised),
            )
            // Recurring status is deliberately omitted here: the local Transaction
            // domain model has no link back to an analytics RecurringTransaction
            // result (that's a merchant/currency-level result, not keyed by
            // transaction id — see core/analytics/model/AnalyticsResult.kt), so
            // showing it would mean inventing data rather than reflecting what
            // the domain actually supports (PR-015 §6).
            Spacer(modifier = Modifier.height(Space24))
        }
    }
}

@Composable
private fun DetailRow(labelRes: Int, value: String) {
    val colors = LocalFinanceColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = Space8), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun signedAmountText(transaction: Transaction): String {
    val sign = if (transaction.amount.minorUnits >= 0) "+" else "-"
    return "$sign${formatMajorUnits(transaction.amount.toMajorUnits())}"
}
