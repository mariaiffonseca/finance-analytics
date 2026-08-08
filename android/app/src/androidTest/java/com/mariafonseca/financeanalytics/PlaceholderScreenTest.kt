package com.mariafonseca.financeanalytics

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class PlaceholderScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun placeholderMessageIsDisplayedOnLaunch() {
        composeTestRule
            .onNodeWithText("Finance Analytics — foundation build")
            .assertExists()
    }
}
