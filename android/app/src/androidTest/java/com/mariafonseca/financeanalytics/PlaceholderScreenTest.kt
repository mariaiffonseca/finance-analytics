package com.mariafonseca.financeanalytics

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class PlaceholderScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun placeholderMessageIsDisplayedOnLaunch() {
        val expectedMessage = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.placeholder_message)

        composeTestRule
            .onNodeWithText(expectedMessage)
            .assertExists()
    }
}
