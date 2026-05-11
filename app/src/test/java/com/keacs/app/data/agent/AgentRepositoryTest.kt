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

    private class FakeAgentNetworkClient : AgentNetworkClient {
        var called = false
        var lastMessage = ""

        override suspend fun chat(
            settings: AgentSettings,
            request: AgentChatRequest,
        ): AgentCallResult {
            called = true
            lastMessage = request.message
            return AgentCallResult.Success(AgentChatResponse(reply = "测试回复"))
        }
    }
}
