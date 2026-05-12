package com.keacs.app.data.agent

import com.keacs.app.domain.agent.AgentModelServiceMode
import com.keacs.app.domain.agent.AgentSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentRepositoryTest {
    @Test
    fun disabledSettingsDoNotCallNetwork() = runTest {
        val client = FakeAgentNetworkClient()
        val repository = AgentRepository(
            settingsProvider = { AgentSettings(enabled = false) },
            client = client,
        )

        val result = repository.sendMessage("这个月花了多少")

        assertTrue(result is AgentCallResult.ConfigurationRequired)
        assertFalse(client.called)
    }

    @Test
    fun validSettingsCallNetwork() = runTest {
        val client = FakeAgentNetworkClient()
        val repository = AgentRepository(
            settingsProvider = {
                AgentSettings(
                    enabled = true,
                    serviceMode = AgentModelServiceMode.CUSTOM,
                    customBaseUrl = "https://api.example.com/v1",
                    customApiKey = "key",
                )
            },
            client = client,
        )

        val result = repository.sendMessage("这个月花了多少")

        assertTrue(result is AgentCallResult.Success)
        assertTrue(client.called)
        assertEquals("这个月花了多少", client.lastMessage)
    }

    @Test
    fun conversationHistoryIsSentWithRequest() = runTest {
        val client = FakeAgentNetworkClient()
        val repository = AgentRepository(
            settingsProvider = {
                AgentSettings(
                    enabled = true,
                    serviceMode = AgentModelServiceMode.CUSTOM,
                    customBaseUrl = "https://api.example.com/v1",
                    customApiKey = "key",
                )
            },
            client = client,
        )

        repository.sendMessage(
            message = "继续分析",
            conversationHistory = listOf(
                AgentConversationTurn("user", "这个月花了多少"),
                AgentConversationTurn("assistant", "本月支出 18 元"),
            ),
        )

        assertEquals(2, client.lastHistory.size)
        assertEquals("assistant", client.lastHistory.last().role)
    }

    @Test
    fun networkFailureCanReturnLocalReadableRecordPreview() = runTest {
        val client = FakeAgentNetworkClient(
            result = AgentCallResult.NetworkFailure("服务器暂时无法连接，请稍后再试。"),
        )
        val repository = AgentRepository(
            settingsProvider = {
                AgentSettings(
                    enabled = true,
                    serviceMode = AgentModelServiceMode.CUSTOM,
                    customBaseUrl = "https://api.example.com/v1",
                    customApiKey = "key",
                )
            },
            client = client,
        )
        val localContext = AgentLocalContext(
            categories = listOf(
                mapOf("name" to "餐饮", "direction" to "EXPENSE"),
                mapOf("name" to "其他", "direction" to "EXPENSE"),
            ),
            accounts = listOf(mapOf("name" to "微信")),
        )

        val result = repository.sendMessage("昨天午饭 18 微信", localContext)

        assertTrue(result is AgentCallResult.Success)
        val response = (result as AgentCallResult.Success).response
        assertTrue(response.reply.contains("请确认"))
        assertEquals("create_record", response.actions.single().type)
        assertEquals(1_800L, response.actions.single().records.single()["amountCent"])
    }

    @Test
    fun highRiskAdviceReturnsBoundaryWithoutNetwork() = runTest {
        val client = FakeAgentNetworkClient()
        val repository = AgentRepository(
            settingsProvider = {
                AgentSettings(
                    enabled = true,
                    serviceMode = AgentModelServiceMode.CUSTOM,
                    customBaseUrl = "https://api.example.com/v1",
                    customApiKey = "key",
                )
            },
            client = client,
        )

        val result = repository.sendMessage("推荐一只收益高的股票")

        assertTrue(result is AgentCallResult.Success)
        assertFalse(client.called)
        assertTrue((result as AgentCallResult.Success).response.reply.contains("不能处理"))
    }

    @Test
    fun officialSuggestionsComeFromNetwork() = runTest {
        val client = FakeAgentNetworkClient(
            suggestions = listOf(AgentSuggestion("看看工资到账了吗", "salary_day")),
        )
        val repository = AgentRepository(
            settingsProvider = {
                AgentSettings(
                    enabled = true,
                    serviceMode = AgentModelServiceMode.OFFICIAL,
                    deviceId = "1234567890abcdef",
                    officialServiceUrl = "https://agent.example.com",
                )
            },
            client = client,
        )

        val suggestions = repository.loadSuggestions(
            today = "2026-05-12",
            recentHistory = listOf(AgentConversationTurn("user", "看看收入")),
            localSummary = mapOf("isLikelySalaryDay" to true),
        )

        assertEquals(listOf("看看工资到账了吗"), suggestions.map { it.text })
        assertTrue(client.suggestionsCalled)
    }

    @Test
    fun suggestionsFallbackToLocalWhenNetworkFails() = runTest {
        val client = FakeAgentNetworkClient(suggestions = emptyList())
        val repository = AgentRepository(
            settingsProvider = {
                AgentSettings(
                    enabled = true,
                    serviceMode = AgentModelServiceMode.OFFICIAL,
                    deviceId = "1234567890abcdef",
                    officialServiceUrl = "https://agent.example.com",
                )
            },
            client = client,
        )

        val suggestions = repository.loadSuggestions(
            today = "2026-05-12",
            recentHistory = emptyList(),
            localSummary = mapOf("isLikelySalaryDay" to true),
        )

        assertTrue(suggestions.any { it.text.contains("工资") })
    }

    private class FakeAgentNetworkClient(
        private val result: AgentCallResult = AgentCallResult.Success(AgentChatResponse(reply = "测试回复")),
        private val suggestions: List<AgentSuggestion> = emptyList(),
    ) : AgentNetworkClient {
        var called = false
        var suggestionsCalled = false
        var lastMessage = ""
        var lastHistory: List<AgentConversationTurn> = emptyList()

        override suspend fun chat(
            settings: AgentSettings,
            request: AgentChatRequest,
        ): AgentCallResult {
            called = true
            lastMessage = request.message
            lastHistory = request.conversationHistory
            return result
        }

        override suspend fun suggestions(
            settings: AgentSettings,
            request: AgentSuggestionRequest,
        ): List<AgentSuggestion> {
            suggestionsCalled = true
            return suggestions
        }
    }
}
