package com.keacs.app.data.agent

import com.keacs.app.domain.agent.AgentModelServiceMode
import com.keacs.app.domain.agent.AgentSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class HttpUrlConnectionAgentClient : AgentNetworkClient {
    override suspend fun chat(
        settings: AgentSettings,
        request: AgentChatRequest,
    ): AgentCallResult = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(settings.chatUrl()).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
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
                return@withContext AgentCallResult.NetworkFailure(status.toUserMessage())
            }
            parseResponse(settings.serviceMode, responseBody)
        } catch (_: IOException) {
            AgentCallResult.NetworkFailure("服务器暂时无法连接，请稍后再试。")
        } catch (_: RuntimeException) {
            AgentCallResult.InvalidResponse("助手返回内容无法识别，请稍后再试。")
        }
    }

    private fun parseResponse(
        serviceMode: AgentModelServiceMode,
        body: String,
    ): AgentCallResult {
        val json = JSONObject(body)
        return when (serviceMode) {
            AgentModelServiceMode.OFFICIAL -> AgentCallResult.Success(
                AgentChatResponse(
                    reply = json.optString("reply"),
                    needsMoreContext = json.optBoolean("needsMoreContext", false),
                    warnings = json.optJSONArray("warnings").toStringList(),
                ),
            )
            AgentModelServiceMode.CUSTOM -> {
                val choices = json.optJSONArray("choices")
                val reply = choices
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    .orEmpty()
                if (reply.isBlank()) {
                    AgentCallResult.InvalidResponse("助手没有返回可展示内容。")
                } else {
                    AgentCallResult.Success(AgentChatResponse(reply = reply))
                }
            }
        }
    }

    private fun AgentChatRequest.toJson(settings: AgentSettings): JSONObject =
        if (settings.serviceMode == AgentModelServiceMode.OFFICIAL) {
            JSONObject()
                .put("clientRequestId", clientRequestId)
                .put("deviceIdHash", deviceIdHash)
                .put("message", message)
                .put("localContext", localContext.toJson())
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
                                .put("content", "你是 Keacs 记账助手，请根据本地上下文回答。查询类问题只回答，不要要求写入。"),
                        )
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
            .put("categories", categories.toJsonArray())
            .put("accounts", accounts.toJsonArray())
            .put("records", records.toJsonArray())
            .put("stats", JSONObject(stats))
            .put("scheduledRecords", scheduledRecords.toJsonArray())

    private fun List<Map<String, Any?>>.toJsonArray(): JSONArray =
        JSONArray().also { array ->
            forEach { item -> array.put(JSONObject(item)) }
        }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) {
            return emptyList()
        }
        return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
    }

    private fun Int.toUserMessage(): String = when (this) {
        401, 403 -> "模型服务拒绝访问，请检查配置。"
        404 -> "模型服务地址不可用，请检查访问地址。"
        429 -> "请求过于频繁，请稍后再试。"
        in 500..599 -> "服务器暂时不可用，请稍后再试。"
        else -> "助手请求失败，请检查网络或配置。"
    }

    private companion object {
        const val TIMEOUT_MILLIS = 15_000
    }
}
