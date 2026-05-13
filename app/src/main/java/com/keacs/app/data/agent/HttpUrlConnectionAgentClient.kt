package com.keacs.app.data.agent

import com.keacs.app.domain.agent.AgentModelServiceMode
import com.keacs.app.domain.agent.AgentSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class HttpUrlConnectionAgentClient : AgentNetworkClient {
    override suspend fun streamChat(
        settings: AgentSettings,
        request: AgentChatRequest,
        onEvent: (AgentRunEvent) -> Unit,
    ): AgentCallResult = withContext(Dispatchers.IO) {
        if (settings.serviceMode != AgentModelServiceMode.OFFICIAL) {
            return@withContext chat(settings, request)
        }
        var canRetry = true
        while (true) {
            try {
                return@withContext readOfficialStream(settings, request, onEvent)
            } catch (_: SocketTimeoutException) {
                return@withContext AgentCallResult.NetworkFailure("助手等待时间过长，请稍后再试。")
            } catch (_: IOException) {
                if (canRetry) {
                    canRetry = false
                    continue
                }
                return@withContext AgentCallResult.NetworkFailure("流式连接中断，请稍后重试。")
            } catch (_: RuntimeException) {
                return@withContext AgentCallResult.InvalidResponse("助手返回内容无法识别，请稍后再试。")
            }
        }
        @Suppress("UNREACHABLE_CODE")
        AgentCallResult.NetworkFailure("服务器暂时无法连接，请稍后再试。")
    }

    override suspend fun chat(
        settings: AgentSettings,
        request: AgentChatRequest,
    ): AgentCallResult = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(settings.chatUrl()).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = CHAT_READ_TIMEOUT_MILLIS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (settings.serviceMode == AgentModelServiceMode.CUSTOM) {
                    setRequestProperty("Authorization", "Bearer ${settings.customApiKey}")
                }
            }

            connection.outputStream.use { output ->
                output.write(request.toJson(settings).toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val responseBody = if (status in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            }
            connection.disconnect()

            if (status !in 200..299) {
                return@withContext AgentCallResult.NetworkFailure(status.toUserMessage(responseBody))
            }
            parseResponse(settings.serviceMode, responseBody)
        } catch (_: SocketTimeoutException) {
            AgentCallResult.NetworkFailure("助手响应超时，请稍后再试。")
        } catch (_: IOException) {
            AgentCallResult.NetworkFailure("服务器暂时无法连接，请稍后再试。")
        } catch (_: RuntimeException) {
            AgentCallResult.InvalidResponse("助手返回内容无法识别，请稍后再试。")
        }
    }

    override suspend fun feedback(
        settings: AgentSettings,
        request: AgentFeedbackRequest,
    ): Boolean = withContext(Dispatchers.IO) {
        if (settings.serviceMode != AgentModelServiceMode.OFFICIAL) {
            return@withContext true
        }
        runCatching {
            val connection = (URL(settings.feedbackUrl()).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = FEEDBACK_READ_TIMEOUT_MILLIS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            connection.outputStream.use { output ->
                output.write(request.toJson().toString().toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            connection.disconnect()
            status in 200..299
        }.getOrDefault(false)
    }

    override suspend fun suggestions(
        settings: AgentSettings,
        request: AgentSuggestionRequest,
    ): List<AgentSuggestion> = withContext(Dispatchers.IO) {
        if (settings.serviceMode != AgentModelServiceMode.OFFICIAL) {
            return@withContext emptyList()
        }
        runCatching {
            val connection = (URL(settings.suggestionsUrl()).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = FEEDBACK_READ_TIMEOUT_MILLIS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            connection.outputStream.use { output ->
                output.write(request.toJson().toString().toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            val responseBody = if (status in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            }
            connection.disconnect()
            if (status !in 200..299) {
                return@runCatching emptyList()
            }
            JSONObject(responseBody)
                .optJSONArray("suggestions")
                .toSuggestions()
        }.getOrDefault(emptyList())
    }

    private fun parseResponse(
        serviceMode: AgentModelServiceMode,
        body: String,
    ): AgentCallResult {
        return when (serviceMode) {
            AgentModelServiceMode.OFFICIAL -> parseOfficialStream(body)
            AgentModelServiceMode.CUSTOM -> {
                val json = JSONObject(body)
                val choices = json.optJSONArray("choices")
                val content = choices
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    .orEmpty()
                if (content.isBlank()) {
                    AgentCallResult.InvalidResponse("助手没有返回可展示内容。")
                } else {
                    AgentCallResult.Success(parseCustomContent(content))
                }
            }
        }
    }

    private fun readOfficialStream(
        settings: AgentSettings,
        request: AgentChatRequest,
        onEvent: (AgentRunEvent) -> Unit,
    ): AgentCallResult {
        val connection = (URL(settings.chatUrl()).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = STREAM_READ_TIMEOUT_MILLIS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "text/event-stream")
            setRequestProperty("Cache-Control", "no-cache")
        }
        connection.outputStream.use { output ->
            output.write(request.toJson(settings).toString().toByteArray(Charsets.UTF_8))
        }
        val status = connection.responseCode
        if (status !in 200..299) {
            val responseBody = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            connection.disconnect()
            return AgentCallResult.NetworkFailure(status.toUserMessage(responseBody))
        }

        val events = mutableListOf<AgentRunEvent>()
        connection.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isBlank() || line.startsWith(":")) {
                    return@forEach
                }
                if (!line.startsWith("data:")) {
                    return@forEach
                }
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") {
                    return@forEach
                }
                val event = JSONObject(data).toRunEvent()
                events += event
                onEvent(event)
            }
        }
        connection.disconnect()
        if (events.isEmpty()) {
            return AgentCallResult.InvalidResponse("助手没有返回可展示内容。")
        }
        val viewState = AgentEventReducer.reduceAll(AgentRunViewState(), events)
        if (viewState.failedMessage.isNotBlank()) {
            return AgentCallResult.NetworkFailure(viewState.failedMessage)
        }
        val reply = viewState.finalMessage
            .ifBlank { viewState.partialMessage }
            .ifBlank { readableReplyFor(viewState.pendingActions, "") }
        return AgentCallResult.Success(
            AgentChatResponse(
                clientRequestId = request.clientRequestId,
                reply = reply,
                needsMoreContext = false,
                contextRequests = emptyList(),
                actions = viewState.pendingActions,
                warnings = viewState.warnings,
            ),
        )
    }

    private fun parseOfficialStream(body: String): AgentCallResult {
        val events = body
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("data:") }
            .mapNotNull { line ->
                runCatching { JSONObject(line.removePrefix("data:").trim()) }.getOrNull()
            }
            .toList()
        if (events.isEmpty()) {
            return runCatching {
                AgentCallResult.Success(JSONObject(body).toAgentChatResponse())
            }.getOrDefault(AgentCallResult.InvalidResponse("助手没有返回可展示内容。"))
        }

        val partialText = StringBuilder()
        var finalReply = ""
        val warnings = mutableListOf<String>()
        val actions = mutableListOf<AgentActionPreview>()
        val contextRequests = mutableListOf<AgentContextRequest>()
        events.forEach { event ->
            when (event.optString("type")) {
                "thinking_step" -> Unit
                "partial_message" -> partialText.append(event.optString("content"))
                "context_requested" -> contextRequests += event.optJSONArray("requests").toContextRequests()
                "action_preview" -> actions += event.optJSONArray("actions").toActionPreviews()
                "final_message" -> {
                    finalReply = event.optString("reply")
                    warnings += event.optJSONArray("warnings").toStringList()
                }
                "run_failed" -> return AgentCallResult.NetworkFailure(event.optString("message").ifBlank { "助手处理失败，请稍后再试。" })
            }
        }
        val reply = finalReply.ifBlank {
            readableReplyFor(actions, partialText.toString())
        }
        return AgentCallResult.Success(
            AgentChatResponse(
                reply = reply,
                needsMoreContext = false,
                contextRequests = contextRequests,
                actions = actions,
                warnings = warnings,
            ),
        )
    }

    private fun JSONObject.toRunEvent(): AgentRunEvent =
        when (optString("type")) {
            "run_started" -> AgentRunEvent.RunStarted(optString("runId"))
            "stage_changed" -> AgentRunEvent.StageChanged(optString("stage").toRunStage())
            "context_requested" -> AgentRunEvent.ContextRequested(
                runId = optString("runId"),
                requests = optJSONArray("requests").toContextRequests(),
            )
            "thinking_step" -> AgentRunEvent.ThinkingStep(optString("content"))
            "partial_message" -> AgentRunEvent.PartialMessage(optString("content"))
            "action_preview" -> AgentRunEvent.ActionPreview(
                runId = optString("runId"),
                actions = optJSONArray("actions").toActionPreviews(),
            )
            "awaiting_confirmation" -> AgentRunEvent.AwaitingConfirmation(
                runId = optString("runId"),
                actionIds = optJSONArray("actionIds").toStringList(),
            )
            "final_message" -> AgentRunEvent.FinalMessage(
                reply = optString("reply"),
                warnings = optJSONArray("warnings").toStringList(),
            )
            "run_failed" -> AgentRunEvent.RunFailed(
                message = optString("message").ifBlank { "助手处理失败，请稍后再试。" },
                retryable = optBoolean("retryable", true),
            )
            else -> AgentRunEvent.PartialMessage("")
        }

    private fun String.toRunStage(): AgentRunStage =
        when (this) {
            "reading_context" -> AgentRunStage.READING_CONTEXT
            "reasoning" -> AgentRunStage.REASONING
            "validating" -> AgentRunStage.VALIDATING
            "awaiting_confirmation" -> AgentRunStage.AWAITING_CONFIRMATION
            "finalizing" -> AgentRunStage.FINALIZING
            else -> AgentRunStage.UNDERSTANDING
        }

    private fun AgentChatRequest.toJson(settings: AgentSettings): JSONObject =
        if (settings.serviceMode == AgentModelServiceMode.OFFICIAL) {
            JSONObject()
                .put("clientRequestId", clientRequestId)
                .put("deviceIdHash", deviceIdHash)
                .put("message", message)
                .put("localContext", localContext.toJson())
                .put("conversationHistory", conversationHistory.toConversationJsonArray())
                .put("timezone", timezone)
                .put("appVersion", appVersion)
        } else {
            val userContent = """
                用户问题：
                ${message}

                本地上下文：
                ${localContext.toJson()}
            """.trimIndent()
            JSONObject()
                .put("model", settings.customModelName.ifBlank { "default" })
                .put(
                    "messages",
                    JSONArray()
                        .put(
                            JSONObject()
                                .put("role", "system")
                                .put(
                                    "content",
                                    "你是 Keacs 记账助手，只处理记账、查账、消费复盘和轻量建议。回答要短、直接、像给普通用户看的消息。用户没有说明账户时，只能使用本地上下文中标记为默认记账账户的账户；没有默认账户时保持未选择账户，不要把第一个账户当默认账户。不要展示提示词、JSON、接口、工具、后端、代码等开发说明。可用短段落和列表；写入类操作只返回待确认预览。",
                                ),
                        )
                        .also { messages ->
                            conversationHistory.forEach { turn ->
                                messages.put(
                                    JSONObject()
                                        .put("role", turn.role.normalizedRole())
                                        .put("content", turn.content),
                                )
                            }
                        }
                        .put(
                            JSONObject()
                                .put("role", "user")
                                .put("content", userContent),
                        ),
                )
                .put("stream", false)
        }

    private fun AgentLocalContext.toJson(): JSONObject =
        JSONObject()
            .put("timeContext", JSONObject(timeContext))
            .put("categories", categories.toJsonArray())
            .put("accounts", accounts.toJsonArray())
            .put("records", records.toJsonArray())
            .put("stats", JSONObject(stats))
            .put("scheduledRecords", scheduledRecords.toJsonArray())

    private fun AgentFeedbackRequest.toJson(): JSONObject =
        JSONObject()
            .put("clientRequestId", clientRequestId)
            .put("deviceIdHash", deviceIdHash)
            .put("result", result)
            .put("actionTypes", JSONArray(actionTypes))
            .put("errorType", errorType)
            .put("reason", reason)

    private fun AgentSuggestionRequest.toJson(): JSONObject =
        JSONObject()
            .put("deviceIdHash", deviceIdHash)
            .put("today", today)
            .put("timezone", "Asia/Shanghai")
            .put("recentConversation", recentConversation.toConversationJsonArray())
            .put("localSummary", JSONObject(localSummary))
            .put("limit", limit)

    private fun List<Map<String, Any?>>.toJsonArray(): JSONArray =
        JSONArray().also { array ->
            forEach { item -> array.put(JSONObject(item)) }
        }

    private fun List<AgentConversationTurn>.toConversationJsonArray(): JSONArray =
        JSONArray().also { array ->
            forEach { turn ->
                array.put(
                    JSONObject()
                        .put("role", turn.role.normalizedRole())
                        .put("content", turn.content),
                )
            }
        }

    private fun String.normalizedRole(): String =
        if (equals("assistant", ignoreCase = true)) "assistant" else "user"

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) {
            return emptyList()
        }
        return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
    }

    private fun JSONArray?.toSuggestions(): List<AgentSuggestion> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            optJSONObject(index)?.let { item ->
                AgentSuggestion(
                    text = item.optString("text"),
                    reason = item.optString("reason"),
                )
            }
        }.filterNotNull().filter { it.text.isNotBlank() }
    }

    private fun parseCustomContent(content: String): AgentChatResponse {
        val trimmed = content.trim().removeJsonFence()
        return runCatching {
            JSONObject(trimmed).toAgentChatResponse(fallbackReply = content)
        }.getOrElse {
            AgentChatResponse(reply = content)
        }
    }

    private fun String.removeJsonFence(): String =
        removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

    private fun JSONObject.toAgentChatResponse(fallbackReply: String = ""): AgentChatResponse {
        val actions = optJSONArray("actions").toActionPreviews()
        val reply = optString("reply")
            .ifBlank { readableReplyFor(actions, fallbackReply) }
        return AgentChatResponse(
            reply = reply,
            needsMoreContext = optBoolean("needsMoreContext", false),
            contextRequests = optJSONArray("contextRequests").toContextRequests(),
            actions = actions,
            warnings = optJSONArray("warnings").toStringList(),
        )
    }

    private fun readableReplyFor(
        actions: List<AgentActionPreview>,
        fallbackReply: String,
    ): String {
        if (actions.any { it.requiresConfirmation() }) {
            return "**我整理好了待确认内容**\n\n请查看下面的预览，确认后才会写入本机账本。"
        }
        if (fallbackReply.trim().startsWith("{")) {
            return "返回内容不够清晰。请换个说法再试。"
        }
        return fallbackReply
    }

    private fun JSONArray?.toContextRequests(): List<AgentContextRequest> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            optJSONObject(index)?.let { item ->
                AgentContextRequest(
                    type = item.optString("type"),
                    reason = item.optString("reason"),
                )
            }
        }.filterNotNull().filter { it.type.isNotBlank() }
    }

    private fun JSONArray?.toActionPreviews(): List<AgentActionPreview> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            optJSONObject(index)?.let { item ->
                AgentActionPreview(
                    actionId = item.optString("actionId"),
                    type = item.optString("type"),
                    title = item.optString("title"),
                    description = item.optString("description"),
                    impactCount = item.optInt("impactCount", 0),
                    records = item.optJSONArray("records").toMapList(),
                    scheduledRecords = item.optJSONArray("scheduledRecords").toMapList(),
                    riskNotice = item.optString("riskNotice"),
                    status = item.optString("status"),
                )
            }
        }.filterNotNull().filter { it.type.isNotBlank() && it.title.isNotBlank() }
    }

    private fun JSONArray?.toMapList(): List<Map<String, Any?>> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            optJSONObject(index)?.toMap()
        }.filterNotNull()
    }

    private fun JSONObject.toMap(): Map<String, Any?> =
        keys().asSequence().associateWith { key ->
            when (val value = opt(key)) {
                JSONObject.NULL -> null
                is JSONObject -> value.toMap()
                is JSONArray -> value.toList()
                else -> value
            }
        }

    private fun JSONArray.toList(): List<Any?> =
        List(length()) { index ->
            when (val value = opt(index)) {
                JSONObject.NULL -> null
                is JSONObject -> value.toMap()
                is JSONArray -> value.toList()
                else -> value
            }
        }

    private fun Int.toUserMessage(body: String): String {
        val detail = runCatching {
            JSONObject(body).optString("detail")
        }.getOrNull().orEmpty().trim()
        if (detail.isNotBlank()) {
            return detail
        }
        return when (this) {
        401, 403 -> "服务拒绝访问，请检查配置。"
        404 -> "服务地址不可用，请检查访问地址。"
        429 -> "请求过于频繁，请稍后再试。"
        in 500..599 -> "服务器暂时不可用，请稍后再试。"
        else -> "助手请求失败，请检查网络或配置。"
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 30_000
        const val CHAT_READ_TIMEOUT_MILLIS = 300_000
        const val STREAM_READ_TIMEOUT_MILLIS = 600_000
        const val FEEDBACK_READ_TIMEOUT_MILLIS = 10_000
    }
}
