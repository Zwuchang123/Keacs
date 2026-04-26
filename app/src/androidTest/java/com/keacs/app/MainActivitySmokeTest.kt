package com.keacs.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
        composeRule.onNodeWithTag("screen-home").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("切换到账单").performClick()
        composeRule.onNodeWithTag("screen-records").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("切换到新增").performClick()
        composeRule.onNodeWithTag("screen-add").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("切换到统计").assertCountEquals(0)

        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNodeWithTag("screen-home").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("切换到统计").performClick()
        composeRule.onNodeWithTag("screen-stats").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("切换到我的").performClick()
        composeRule.onNodeWithTag("screen-mine").assertIsDisplayed()
    }
}
