package com.mariafonseca.financeanalytics.features.overview.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsResult
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsSummary
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsUiState
import com.mariafonseca.financeanalytics.core.analytics.model.Insight
import com.mariafonseca.financeanalytics.core.designsystem.FinanceAnalyticsTheme
import org.junit.Rule
import org.junit.Test

/**
 * Isolated Compose coverage of the Overview dashboard (PR-015 §6), composing
 * [OverviewContent] directly with a hand-built [OverviewUiState] — same
 * convention as InsightsScreenTest.
 */
class OverviewScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val overview = MonthlyOverview(
        monthLabel = "March 2026",
        totalSpent = 251.49,
        totalIncome = 500.0,
        totalSaved = 248.51,
        categoryBreakdown = listOf(
            CategorySlice("Groceries", 61.30, 0.7),
            CategorySlice("Transport", 22.40, 0.3),
        ),
        monthlyTrend = listOf(MonthlyTrendPoint("Feb", 363.54), MonthlyTrendPoint("Mar", 251.49)),
    )

    @Test
    fun placeholderStateShowsWhenThereIsNoLocalData() {
        setContent(OverviewUiState())

        composeTestRule.onNodeWithText("Your spending overview will appear here once you import transactions.")
            .assertIsDisplayed()
    }

    @Test
    fun populatedStateShowsTheMonthlyHeadlineNumbers() {
        setContent(OverviewUiState(monthlyOverview = overview))

        composeTestRule.onNodeWithText("March 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("251.49").assertIsDisplayed()
        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()
    }

    @Test
    fun successAnalyticsStateShowsTheTopInsight() {
        val result = AnalyticsResult(
            summary = AnalyticsSummary(0, 0, 0, 0.0, 0.0, 0.0, null, null, 0, 0),
            insights = listOf(
                Insight(
                    id = "spending_trend:2026-03",
                    type = "spending_trend",
                    title = "Spending decreased",
                    description = "Your spending decreased 31% compared with last month.",
                    severity = "NOTICE",
                    confidence = 0.76,
                    relatedTransactionIds = emptyList(),
                    category = null,
                    merchant = null,
                    amount = null,
                    comparisonPeriod = "2026-03",
                ),
            ),
            anomalies = emptyList(),
            recurring = emptyList(),
        )
        setContent(OverviewUiState(monthlyOverview = overview, analyticsState = AnalyticsUiState.Success(result)))

        composeTestRule.onNodeWithText("Spending decreased").assertIsDisplayed()
    }

    @Test
    fun unavailableAnalyticsStateOffersRetryWithoutHidingLocalNumbers() {
        var retried = false
        composeTestRule.setContent {
            FinanceAnalyticsTheme {
                OverviewContent(
                    uiState = OverviewUiState(monthlyOverview = overview, analyticsState = AnalyticsUiState.Unavailable),
                    onRetryAnalysis = { retried = true },
                )
            }
        }

        composeTestRule.onNodeWithText("March 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("Analytics unavailable").assertIsDisplayed()
        composeTestRule.onNodeWithText("Try again").performClick()

        assert(retried)
    }

    private fun setContent(uiState: OverviewUiState) {
        composeTestRule.setContent {
            FinanceAnalyticsTheme {
                OverviewContent(uiState = uiState, onRetryAnalysis = {})
            }
        }
    }
}
