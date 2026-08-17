package com.mariafonseca.financeanalytics.features.insights.presentation

import androidx.compose.ui.test.assertIsDisplayed
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
import com.mariafonseca.financeanalytics.core.designsystem.FinanceAnalyticsTheme
import org.junit.Rule
import org.junit.Test

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

    private fun setContent(uiState: InsightsUiState) {
        composeTestRule.setContent {
            FinanceAnalyticsTheme {
                InsightsContent(uiState = uiState, onFilterSelected = {}, onRetryAnalysis = {})
            }
        }
    }
}
