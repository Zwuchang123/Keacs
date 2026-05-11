package com.keacs.app.domain.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentSettingsTest {
    @Test
    fun disabledSettingsCannotRequest() {
        val validation = AgentSettings(enabled = false).validateForRequest()

        assertFalse(validation.canRequest)
        assertEquals("在线助手未启用，请先到设置页开启。", validation.message)
    }

    @Test
    fun customSettingsRequireBaseUrlAndApiKey() {
        val missingBaseUrl = AgentSettings(
            enabled = true,
            serviceMode = AgentModelServiceMode.CUSTOM,
            customApiKey = "key",
        ).validateForRequest()
        val missingApiKey = AgentSettings(
            enabled = true,
            serviceMode = AgentModelServiceMode.CUSTOM,
            customBaseUrl = "https://api.example.com/v1",
        ).validateForRequest()

        assertFalse(missingBaseUrl.canRequest)
        assertEquals("请先填写自定义模型访问地址。", missingBaseUrl.message)
        assertFalse(missingApiKey.canRequest)
        assertEquals("请先填写自定义模型 API Key。", missingApiKey.message)
    }

    @Test
    fun customSettingsAllowHttpsAndLocalDebugUrl() {
        val https = AgentSettings(
            enabled = true,
            serviceMode = AgentModelServiceMode.CUSTOM,
            customBaseUrl = "https://api.example.com/v1",
            customApiKey = "key",
        ).validateForRequest()
        val local = AgentSettings(
            enabled = true,
            serviceMode = AgentModelServiceMode.CUSTOM,
            customBaseUrl = "http://127.0.0.1:8000/v1",
            customApiKey = "key",
        ).validateForRequest()

        assertTrue(https.canRequest)
        assertTrue(local.canRequest)
    }
}
