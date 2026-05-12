package com.keacs.app.data.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentTaskModelsTest {
    @Test
    fun reducerKeepsStageAndPendingActionFromEvents() {
        val action = AgentActionPreview(
            actionId = "action-1",
            type = "create_record",
            title = "新增账目",
            records = listOf(mapOf("amountCent" to 1_800L)),
        )

        val state = AgentEventReducer.reduceAll(
            AgentRunViewState(),
            listOf(
                AgentRunEvent.RunStarted("run-1"),
                AgentRunEvent.StageChanged(AgentRunStage.READING_CONTEXT),
                AgentRunEvent.ContextRequested("run-1", listOf(AgentContextRequest("month_stats", "读取本月统计"))),
                AgentRunEvent.ActionPreview("run-1", listOf(action)),
                AgentRunEvent.AwaitingConfirmation("run-1", listOf("action-1")),
            ),
        )

        assertEquals("run-1", state.runId)
        assertEquals(AgentRunStage.AWAITING_CONFIRMATION, state.stage)
        assertEquals(1, state.pendingActions.size)
        assertEquals("action-1", state.pendingActions.single().actionId)
        assertTrue(state.contextNotice.contains("读取本月统计"))
    }

    @Test
    fun runStoreBlocksRepeatedActionExecution() {
        val store = AgentRunStore()
        val action = AgentActionPreview(
            actionId = "once-1",
            type = "delete_record",
            title = "删除账目",
            records = listOf(mapOf("id" to 1L)),
        )

        store.savePendingAction("run-1", action)

        assertTrue(store.markActionConfirmed("once-1"))
        assertFalse(store.markActionConfirmed("once-1"))
        assertTrue(store.pendingActions().isEmpty())
    }

    @Test
    fun runStoreUpdatesEditedPendingAction() {
        val store = AgentRunStore()
        val action = AgentActionPreview(
            actionId = "once-1",
            type = "create_record",
            title = "新增账目",
            records = listOf(mapOf("amountCent" to 1_800L)),
        )

        store.savePendingAction("run-1", action)
        store.updatePendingAction(action.copy(records = listOf(mapOf("amountCent" to 2_000L))))

        assertEquals(2_000L, store.pendingActions().single().records.single()["amountCent"])
    }

    @Test
    fun suggestionProviderReturnsDynamicShortSuggestions() {
        val suggestions = AgentSuggestionProvider().buildLocalSuggestions(
            today = "2026-05-31",
            recentMessages = listOf("这个月餐饮花了多少"),
            localSummary = mapOf(
                "hasLargeExpense" to true,
                "topExpenseCategory" to "餐饮",
            ),
            limit = 4,
        )

        assertTrue(suggestions.size in 2..4)
        assertEquals(suggestions.size, suggestions.map { it.text }.toSet().size)
        assertTrue(suggestions.any { it.text.contains("餐饮") || it.text.contains("月末") })
    }

    @Test
    fun suggestionProviderUsesSalaryDayLargeExpenseAndFestival() {
        val suggestions = AgentSuggestionProvider().buildLocalSuggestions(
            today = "2026-10-01",
            recentMessages = listOf("上次帮我看了交通"),
            localSummary = mapOf(
                "isLikelySalaryDay" to true,
                "hasLargeExpense" to true,
                "festivalName" to "国庆",
                "topExpenseCategory" to "交通",
            ),
            limit = 4,
        )

        val texts = suggestions.map { it.text }
        assertEquals(4, texts.size)
        assertTrue(texts.any { it.contains("工资") })
        assertTrue(texts.any { it.contains("国庆") })
        assertTrue(texts.any { it.contains("大额") })
        assertTrue(texts.none { it.length > 18 })
    }
}
