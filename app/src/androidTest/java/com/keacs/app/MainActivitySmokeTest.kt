package com.keacs.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationSwitchesAcrossMainPages() {
        enterMainIfWelcomeShown()

        assertScreenDisplayed("screen-home")

        composeRule.onNodeWithContentDescription("切换到账单").performClick()
        assertScreenDisplayed("screen-records")

        composeRule.onNodeWithContentDescription("切换到新增").performClick()
        assertScreenDisplayed("screen-add")
        composeRule.onAllNodesWithContentDescription("切换到统计").assertCountEquals(0)

        composeRule.onNodeWithContentDescription("返回").performClick()
        assertScreenDisplayed("screen-records")

        composeRule.onNodeWithContentDescription("切换到统计").performClick()
        assertScreenDisplayed("screen-stats")

        composeRule.onNodeWithContentDescription("切换到我的").performClick()
        assertScreenDisplayed("screen-mine")
    }

    private fun enterMainIfWelcomeShown() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("screen-home").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag("welcome-start").fetchSemanticsNodes().isNotEmpty()
        }

        if (composeRule.onAllNodesWithTag("welcome-start").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("welcome-start").performClick()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("screen-home").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertScreenDisplayed(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }
}
