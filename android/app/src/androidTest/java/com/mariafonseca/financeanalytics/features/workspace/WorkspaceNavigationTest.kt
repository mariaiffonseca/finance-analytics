package com.mariafonseca.financeanalytics.features.workspace

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.mariafonseca.financeanalytics.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class WorkspaceNavigationTest {

    // This test boots the real app (MainActivity), including its real,
    // persistent Room database — unlike TransactionDaoTest's isolated
    // in-memory instance. Since AppDataViewModel now reflects already-
    // persisted transactions on startup (so a returning user with local
    // data skips the Empty screen — see its own doc comment), a database
    // left non-empty by a previous run or manual session would otherwise
    // make this test's Empty-screen assertion flaky. order = 0 runs this
    // before composeTestRule (order = 1) launches the activity.
    @get:Rule(order = 0)
    val clearDatabase = object : TestWatcher() {
        override fun starting(description: Description) {
            InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase("finance_analytics.db")
        }
    }

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun navigatingFromEmptyThroughImportReachesAppShellWithWorkingTabs() {
        composeTestRule.onNodeWithText("Understand where your money goes.").assertExists()

        composeTestRule.onNodeWithText("Import CSV").performClick()
        composeTestRule.onNodeWithText("Import transactions").assertExists()

        // Exercises the skip path deliberately: this test verifies workspace
        // navigation (Empty -> Import -> AppShell tabs), not CSV parsing —
        // that's CsvImportPipelineTest/ImportViewModelTest's job. Skipping
        // keeps AppDataState.NoData, hence the "will appear once you import"
        // placeholder assertions below.
        composeTestRule.onNodeWithText("Continue without importing").performClick()
        composeTestRule.onNodeWithText(
            "Your spending overview will appear here once you import transactions.",
        ).assertExists()

        composeTestRule.onNodeWithText("Insights").performClick()
        composeTestRule.onNodeWithText(
            "Insights will appear here once you import transactions.",
        ).assertExists()
        composeTestRule.onNodeWithText("Anomalies").performScrollTo().assertExists()

        composeTestRule.onNodeWithText("Transactions").performClick()
        composeTestRule.onNodeWithText(
            "Your transactions will appear here once you import data.",
        ).assertExists()
    }
}
