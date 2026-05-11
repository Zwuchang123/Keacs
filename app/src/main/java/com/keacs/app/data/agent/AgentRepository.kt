package com.keacs.app.data.agent

import com.keacs.app.data.local.PreferencesManager
import com.keacs.app.domain.agent.AgentModelServiceMode
import com.keacs.app.domain.agent.AgentSettings
import com.keacs.app.domain.agent.validateForRequest
import kotlinx.coroutines.flow.first

class AgentRepository(
    private val settingsProvider: suspend () -> AgentSettings,
    private val client: AgentNetworkClient = HttpUrlConnectionAgentClient(),
) {
    constructor(
        preferencesManager: PreferencesManager,
        client: AgentNetworkClient = HttpUrlConnectionAgentClient(),
    ) : this(
        settingsProvider = { preferencesManager.agentSettings.first() },
        client = client,
    )

    suspend fun sendMessage(
        message: String,
        localContext: AgentLocalContext = AgentLocalContext(),
    ): AgentCallResult {
        val settings = settingsProvider()
        val validation = settings.validateForRequest()
        if (!validation.canRequest) {
            return AgentCallResult.ConfigurationRequired(validation.message.orEmpty())
        }
        if (message.isBlank()) {
            return AgentCallResult.ConfigurationRequired("请输入要发送的内容。")
        }

        val request = AgentChatRequest(
            clientRequestId = java.util.UUID.randomUUID().toString(),
            deviceIdHash = settings.deviceId,
            message = message.trim(),
            localContext = localContext,
            appVersion = com.keacs.app.BuildConfig.VERSION_NAME,
        )
        return client.chat(settings, request)
    }
}

interface AgentNetworkClient {
    suspend fun chat(settings: AgentSettings, request: AgentChatRequest): AgentCallResult
}

data class AgentChatRequest(
    val clientRequestId: String,
    val deviceIdHash: String,
    val message: String,
    val localContext: AgentLocalContext,
    val timezone: String = "Asia/Shanghai",
    val appVersion: String,
)

data class AgentLocalContext(
    val categories: List<Map<String, Any?>> = emptyList(),
    val accounts: List<Map<String, Any?>> = emptyList(),
    val records: List<Map<String, Any?>> = emptyList(),
    val stats: Map<String, Any?> = emptyMap(),
    val scheduledRecords: List<Map<String, Any?>> = emptyList(),
)

data class AgentChatResponse(
    val reply: String,
    val needsMoreContext: Boolean = false,
    val contextRequests: List<AgentContextRequest> = emptyList(),
    val actions: List<AgentActionPreview> = emptyList(),
    val warnings: List<String> = emptyList(),
)

data class AgentContextRequest(
    val type: String,
    val reason: String = "",
)

data class AgentActionPreview(
    val type: String,
    val title: String,
    val description: String = "",
    val impactCount: Int = 0,
    val records: List<Map<String, Any?>> = emptyList(),
    val scheduledRecords: List<Map<String, Any?>> = emptyList(),
    val riskNotice: String = "",
)

sealed interface AgentCallResult {
    data class Success(val response: AgentChatResponse) : AgentCallResult
    data class ConfigurationRequired(val message: String) : AgentCallResult
    data class NetworkFailure(val message: String) : AgentCallResult
    data class InvalidResponse(val message: String) : AgentCallResult
}

fun AgentActionPreview.requiresConfirmation(): Boolean =
    type in setOf(
        "create_record",
        "update_record",
        "delete_record",
        "batch_update_records",
        "create_scheduled_record",
        "update_scheduled_record",
        "disable_scheduled_record",
    )

internal fun AgentSettings.chatUrl(): String {
    val baseUrl = endpointBaseUrl
    return when (serviceMode) {
        AgentModelServiceMode.OFFICIAL -> "$baseUrl/api/agent/chat"
        AgentModelServiceMode.CUSTOM -> "$baseUrl/chat/completions"
    }
}
