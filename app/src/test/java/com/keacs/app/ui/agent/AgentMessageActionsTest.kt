package com.keacs.app.ui.agent

import com.keacs.app.data.agent.AgentActionPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentMessageActionsTest {
    @Test
    fun feedbackClickTogglesAndSwitchesSelection() {
        assertEquals("like", toggleAgentFeedback("", "like"))
        assertEquals("", toggleAgentFeedback("like", "like"))
        assertEquals("dislike", toggleAgentFeedback("like", "dislike"))
    }

    @Test
    fun guidanceCanToggleVisibleState() {
        assertFalse(toggleAgentGuidance(true))
        assertTrue(toggleAgentGuidance(false))
    }

    @Test
    fun onlyLastAssistantMessageCanRegenerate() {
        val messages = listOf(
            AgentMessage(1, AgentMessageRole.USER, "这个月花了多少"),
            AgentMessage(2, AgentMessageRole.ASSISTANT, "第一次回复"),
            AgentMessage(3, AgentMessageRole.USER, "继续说"),
            AgentMessage(4, AgentMessageRole.ASSISTANT, "第二次回复"),
        )

        assertEquals(4L, messages.lastRegenerableAssistantId())
        assertFalse(messages.canRegenerateMessage(2))
        assertTrue(messages.canRegenerateMessage(4))
    }

    @Test
    fun regenerateKeepsOriginalAssistantCardPosition() {
        val messages = listOf(
            AgentMessage(1, AgentMessageRole.USER, "分析本月"),
            AgentMessage(2, AgentMessageRole.ASSISTANT, "旧回复", feedback = "like"),
        )

        val updated = messages.replaceAssistantMessage(
            messageId = 2,
            text = "新回复",
            elapsedMillis = 1200L,
        )

        assertEquals(2, updated.size)
        assertEquals(2L, updated.last().id)
        assertEquals("新回复", updated.last().text)
        assertEquals("", updated.last().feedback)
        assertEquals(1200L, updated.last().elapsedMillis)
    }

    @Test
    fun returnsNullWhenNoAssistantMessageCanRegenerate() {
        assertNull(listOf(AgentMessage(1, AgentMessageRole.USER, "你好")).lastRegenerableAssistantId())
    }

    @Test
    fun previewItemsSplitRecordsAndSchedulesForSwitching() {
        val action = AgentActionPreview(
            type = "batch_update_records",
            title = "批量修改",
            records = listOf(
                mapOf("type" to "EXPENSE", "amountCent" to 1_000L),
                mapOf("type" to "EXPENSE", "amountCent" to 2_000L),
            ),
            scheduledRecords = listOf(
                mapOf("frequency" to "MONTHLY", "amountCent" to 3_000L),
            ),
        )

        val items = action.previewItems()

        assertEquals(3, items.size)
        assertEquals("账目 1/2", items[0].pageLabel)
        assertEquals("账目 2/2", items[1].pageLabel)
        assertEquals("定时 1/1", items[2].pageLabel)
    }
}
