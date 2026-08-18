package com.mariafonseca.financeanalytics.features.insights.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.mariafonseca.financeanalytics.R
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsResult
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsSummary
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsUiState
import com.mariafonseca.financeanalytics.core.analytics.model.Anomaly
import com.mariafonseca.financeanalytics.core.analytics.model.Insight
import com.mariafonseca.financeanalytics.core.analytics.model.RecurringTransaction
import com.mariafonseca.financeanalytics.core.designsystem.FinanceAnalyticsTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Isolated Compose coverage of the analytics states (PR-014 §15/§16),
 * composing [InsightsContent] directly with a hand-built [InsightsUiState]
 * rather than booting the full Koin graph and a real network stack — the
 * repository-level end-to-end path is already covered by
 * AnalyticsApiIntegrationTest.
 */
class InsightsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val tryAgainLabel: String
        get() = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.action_try_again)

    @Test
    fun idleStateShowsThePlaceholderMessage() {
        setContent(InsightsUiState(analyticsState = AnalyticsUiState.Idle))

        composeTestRule.onNodeWithText("Insights will appear here once you import transactions.").assertIsDisplayed()
    }

    @Test
    fun loadingStateDoesNotShowThePlaceholderMessage() {
        setContent(InsightsUiState(analyticsState = AnalyticsUiState.Loading))

        composeTestRule.onNodeWithText("Insights will appear here once you import transactions.")
            .assertDoesNotExist()
    }

    @Test
    fun successStateShowsInsightsAndAnomalies() {
        val result = AnalyticsResult(
            summary = AnalyticsSummary(
                transactionCount = 1,
                incomeCount = 0,
                expenseCount = 1,
                totalIncome = 0.0,
                totalExpenses = -420.0,
                netSavings = -420.0,
                dateRangeStart = null,
                dateRangeEnd = null,
                uniqueMerchantCount = 1,
                uniqueCategoryCount = 1,
            ),
            insights = listOf(
                Insight(
                    id = "insight-1",
                    type = "negative",
                    title = "Restaurant spending increased",
                    description = "Restaurant spending increased by 24% compared to last month.",
                    severity = "warning",
                    confidence = 0.9,
                    relatedTransactionIds = emptyList(),
                    category = "Restaurants",
                    merchant = null,
                    amount = 420.0,
                    comparisonPeriod = "last_month",
                ),
            ),
            anomalies = listOf(
                Anomaly(
                    transactionId = "1",
                    anomalyScore = 3.1,
                    isAnomaly = true,
                    method = "z-score",
                    reason = "Amount is far above the merchant's usual range.",
                ),
            ),
            recurring = emptyList(),
        )
        setContent(InsightsUiState(analyticsState = AnalyticsUiState.Success(result)))

        composeTestRule.onNodeWithText("Restaurant spending increased").assertIsDisplayed()
        composeTestRule.onNodeWithText("Amount is far above the merchant's usual range.").assertIsDisplayed()
    }

    @Test
    fun successStateWithNoRecurringTransactionsShowsTheEmptyMessage() {
        val result = AnalyticsResult(
            summary = AnalyticsSummary(0, 0, 0, 0.0, 0.0, 0.0, null, null, 0, 0),
            insights = emptyList(),
            anomalies = emptyList(),
            recurring = emptyList(),
        )
        setContent(InsightsUiState(analyticsState = AnalyticsUiState.Success(result)))

        composeTestRule.onNodeWithText("No recurring transactions detected.").assertIsDisplayed()
    }

    @Test
    fun unavailableStateOffersRetryWithoutBlockingTheRestOfTheScreen() {
        var retried = false
        composeTestRule.setContent {
            FinanceAnalyticsTheme {
                InsightsContent(
                    uiState = InsightsUiState(analyticsState = AnalyticsUiState.Unavailable),
                    onFilterSelected = {},
                    onRetryAnalysis = { retried = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Analytics unavailable").assertIsDisplayed()
        composeTestRule.onNodeWithText(tryAgainLabel).performClick()

        assert(retried)
    }

    @Test
    fun errorStateOffersRetry() {
        var retried = false
        composeTestRule.setContent {
            FinanceAnalyticsTheme {
                InsightsContent(
                    uiState = InsightsUiState(analyticsState = AnalyticsUiState.Error),
                    onFilterSelected = {},
                    onRetryAnalysis = { retried = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Analytics request failed").assertIsDisplayed()
        composeTestRule.onNodeWithText(tryAgainLabel).performClick()

        assert(retried)
    }

    /**
     * Interactive coverage of the insights tag filtering (PR-015 §7): the
     * screen owns [InsightsUiState.selectedFilter] itself here, exactly as
     * [InsightsViewModel] does in production, so clicking a filter chip
     * exercises the same state update -> recomposition path a real user
     * triggers rather than just calling [AnalyticsResult.viewFor] directly.
     */
    private val filteringResult = AnalyticsResult(
        summary = AnalyticsSummary(0, 0, 0, 0.0, 0.0, 0.0, null, null, 0, 0),
        insights = listOf(
            Insight(
                id = "spending_trend:1",
                type = "spending_trend",
                title = "Spending increased",
                description = "Your spending increased 20% compared with last month.",
                severity = "NOTICE",
                confidence = 0.8,
                relatedTransactionIds = emptyList(),
                category = null,
                merchant = null,
                amount = null,
                comparisonPeriod = "2026-03",
            ),
        ),
        anomalies = listOf(
            Anomaly(
                transactionId = "9",
                anomalyScore = 5.0,
                isAnomaly = true,
                method = "z-score",
                reason = "Amount is far above the merchant's usual range.",
            ),
        ),
        recurring = listOf(
            RecurringTransaction(
                merchant = "Netflix",
                currency = "EUR",
                isRecurring = true,
                classification = "Recurring",
                confidenceScore = 0.9,
                frequency = "monthly",
                occurrences = 4,
                medianAmount = -15.99,
                amountVariation = 0.0,
                medianIntervalDays = 30.0,
                intervalVariation = 0.0,
                firstSeen = LocalDate.of(2026, 1, 1),
                lastSeen = LocalDate.of(2026, 4, 1),
                reason = "Netflix appears every ~30 days with a consistent amount.",
                transactionIds = listOf("1", "2", "3", "4"),
            ),
        ),
    )

    @Test
    fun selectingTheAnomaliesFilterHidesInsightsAndRecurringContent() {
        setStatefulContent(AnalyticsUiState.Success(filteringResult))

        composeTestRule.onNodeWithText("Amount is far above the merchant's usual range.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your spending increased 20% compared with last month.").assertIsDisplayed()

        clickFilterChip("Anomalies")

        composeTestRule.onNodeWithText("Amount is far above the merchant's usual range.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your spending increased 20% compared with last month.").assertDoesNotExist()
        composeTestRule.onNodeWithText("Netflix").assertDoesNotExist()
    }

    @Test
    fun selectingARecentFilterAfterANarrowFilterRestoresEverySection() {
        setStatefulContent(AnalyticsUiState.Success(filteringResult))

        clickFilterChip("Recurring")
        composeTestRule.onNodeWithText("Netflix").assertIsDisplayed()
        composeTestRule.onNodeWithText("Amount is far above the merchant's usual range.").assertDoesNotExist()

        clickFilterChip("Recent")

        composeTestRule.onNodeWithText("Netflix").assertIsDisplayed()
        composeTestRule.onNodeWithText("Amount is far above the merchant's usual range.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your spending increased 20% compared with last month.").assertIsDisplayed()
    }

    @Test
    fun selectingASpendingFilterWithNoMatchesShowsTheFilteredEmptyState() {
        setStatefulContent(AnalyticsUiState.Success(filteringResult))

        clickFilterChip("Spending")

        composeTestRule.onNodeWithText("No insights match this filter.").assertIsDisplayed()
    }

    /**
     * Some filter labels (Anomalies, Recurring) also appear verbatim as a
     * section header or a recurring item's classification text, so a plain
     * [onNodeWithText] click can match more than one node. The filter chip
     * is the only match with a click action (`FinanceFilterChip`'s
     * `.selectable`), which disambiguates it from that other, unclickable
     * text.
     */
    private fun clickFilterChip(label: String) {
        composeTestRule.onNode(hasText(label) and hasClickAction()).performClick()
    }

    private fun setStatefulContent(analyticsState: AnalyticsUiState) {
        composeTestRule.setContent {
            var uiState by remember { mutableStateOf(InsightsUiState(analyticsState = analyticsState)) }
            FinanceAnalyticsTheme {
                InsightsContent(
                    uiState = uiState,
                    onFilterSelected = { filter -> uiState = uiState.copy(selectedFilter = filter) },
                    onRetryAnalysis = {},
                )
            }
        }
    }

    private fun setContent(uiState: InsightsUiState) {
        composeTestRule.setContent {
            FinanceAnalyticsTheme {
                InsightsContent(uiState = uiState, onFilterSelected = {}, onRetryAnalysis = {})
            }
        }
    }
}
