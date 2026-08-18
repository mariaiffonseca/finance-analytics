package com.mariafonseca.financeanalytics.features.transactions.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.mariafonseca.financeanalytics.core.common.Money
import com.mariafonseca.financeanalytics.core.designsystem.FinanceAnalyticsTheme
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Isolated Compose coverage of the Transactions screen (PR-015 §6), composing
 * [TransactionsContent] directly with a hand-built, self-updating
 * [TransactionsUiState] — same convention as InsightsScreenTest's stateful
 * filter-interaction tests.
 */
class TransactionsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val transactions = listOf(
        Transaction(1, LocalDate.of(2026, 3, 1), "Coffee Corner", Money(-450), "Food"),
        Transaction(2, LocalDate.of(2026, 3, 2), "Continente", Money(-6130), "Groceries"),
    )

    @Test
    fun placeholderStateShowsWhenThereAreNoTransactions() {
        setStatefulContent(TransactionsUiState())

        composeTestRule.onNodeWithText("Your transactions will appear here once you import data.").assertIsDisplayed()
    }

    @Test
    fun populatedStateListsEveryTransaction() {
        setStatefulContent(TransactionsUiState(transactions = transactions))

        composeTestRule.onNodeWithText("Coffee Corner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continente").assertIsDisplayed()
    }

    @Test
    fun searchFiltersTheDisplayedTransactionsByMerchant() {
        setStatefulContent(TransactionsUiState(transactions = transactions))

        composeTestRule.onNodeWithText("Search transactions").performTextInput("Coffee")

        composeTestRule.onNodeWithText("Coffee Corner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continente").assertDoesNotExist()
    }

    @Test
    fun categoryFilterNarrowsToMatchingTransactions() {
        setStatefulContent(
            TransactionsUiState(transactions = transactions, categories = listOf("Food", "Groceries")),
        )

        // "Groceries" is both a filter chip and a transaction row's category
        // text (both are clickable, so hasClickAction() alone can't tell
        // them apart) — FinanceFilterChip's Role.Tab semantics uniquely
        // identify the chip.
        composeTestRule.onNode(hasText("Groceries") and hasTabRole()).performClick()

        composeTestRule.onNodeWithText("Continente").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coffee Corner").assertDoesNotExist()
    }

    @Test
    fun tappingATransactionOpensItsDetailSheet() {
        setStatefulContent(TransactionsUiState(transactions = transactions))

        composeTestRule.onNodeWithText("Coffee Corner").performClick()

        composeTestRule.onNodeWithText("Date").assertIsDisplayed()
        composeTestRule.onNodeWithText("Category").assertIsDisplayed()
    }

    private fun hasTabRole(): SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private fun setStatefulContent(initial: TransactionsUiState) {
        composeTestRule.setContent {
            var uiState by remember { mutableStateOf(initial) }
            FinanceAnalyticsTheme {
                TransactionsContent(
                    uiState = uiState,
                    onSearchQueryChanged = { query -> uiState = uiState.copy(searchQuery = query) },
                    onCategorySelected = { category -> uiState = uiState.copy(selectedCategory = category) },
                    onTransactionSelected = { transaction -> uiState = uiState.copy(selectedTransaction = transaction) },
                    onTransactionDetailDismissed = { uiState = uiState.copy(selectedTransaction = null) },
                )
            }
        }
    }
}
