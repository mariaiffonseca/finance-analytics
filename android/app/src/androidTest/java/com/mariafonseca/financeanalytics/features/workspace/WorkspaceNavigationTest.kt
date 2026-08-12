package com.mariafonseca.financeanalytics.features.workspace

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.mariafonseca.financeanalytics.MainActivity
import org.junit.Rule
import org.junit.Test

class WorkspaceNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun navigatingFromEmptyThroughImportReachesAppShellWithWorkingTabs() {
        composeTestRule.onNodeWithText("Understand where your money goes.").assertExists()

        composeTestRule.onNodeWithText("Import CSV").performClick()
        composeTestRule.onNodeWithText("Import transactions").assertExists()

        composeTestRule.onNodeWithText("Continue").performClick()
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
