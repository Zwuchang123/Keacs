package com.keacs.app.ui.agent

import com.keacs.app.data.agent.AgentActionPreview
import org.json.JSONArray
import org.json.JSONObject

fun encodeAgentMessages(messages: List<AgentMessage>): String =
    JSONArray().also { array ->
        messages.forEach { message ->
            array.put(message.toJson())
        }
    }.toString()

fun decodeAgentMessages(snapshot: String): List<AgentMessage> =
    runCatching {
        val array = JSONArray(snapshot)
        List(array.length()) { index ->
            array.optJSONObject(index)?.toAgentMessage()
        }.filterNotNull()
    }.getOrDefault(emptyList())

private fun AgentMessage.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("role", role.name)
        .put("text", text)
        .put("actions", actions.toJsonArray())
        .put("warnings", JSONArray(warnings))
        .put("thinkingSteps", JSONArray(thinkingSteps))
        .put("thinkingExpanded", thinkingExpanded)
        .put("isStreaming", isStreaming)
        .put("elapsedMillis", elapsedMillis ?: JSONObject.NULL)
        .put("feedback", feedback)

private fun JSONObject.toAgentMessage(): AgentMessage =
    AgentMessage(
        id = optLong("id"),
        role = runCatching { AgentMessageRole.valueOf(optString("role")) }
            .getOrDefault(AgentMessageRole.ASSISTANT),
        text = optString("text"),
        actions = optJSONArray("actions").toActionPreviews(),
        warnings = optJSONArray("warnings").toStringList(),
        thinkingSteps = optJSONArray("thinkingSteps").toStringList(),
        thinkingExpanded = optBoolean("thinkingExpanded", true),
        isStreaming = optBoolean("isStreaming", false),
        elapsedMillis = if (isNull("elapsedMillis")) null else optLong("elapsedMillis"),
        feedback = optString("feedback"),
    )

private fun List<AgentActionPreview>.toJsonArray(): JSONArray =
    JSONArray().also { array ->
        forEach { action ->
            array.put(
                JSONObject()
                    .put("type", action.type)
                    .put("actionId", action.actionId)
                    .put("title", action.title)
                    .put("description", action.description)
                    .put("impactCount", action.impactCount)
                    .put("records", action.records.toMapJsonArray())
                    .put("scheduledRecords", action.scheduledRecords.toMapJsonArray())
                    .put("riskNotice", action.riskNotice)
                    .put("status", action.status),
            )
        }
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

private fun List<Map<String, Any?>>.toMapJsonArray(): JSONArray =
    JSONArray().also { array ->
        forEach { item -> array.put(item.toJsonObject()) }
    }

private fun Map<String, Any?>.toJsonObject(): JSONObject =
    JSONObject().also { json ->
        forEach { (key, value) ->
            json.put(key, value.toJsonValue())
        }
    }

private fun Any?.toJsonValue(): Any? =
    when (this) {
        null -> JSONObject.NULL
        is Map<*, *> -> JSONObject().also { json ->
            forEach { (key, value) ->
                if (key is String) json.put(key, value.toJsonValue())
            }
        }
        is List<*> -> JSONArray().also { array ->
            forEach { item -> array.put(item.toJsonValue()) }
        }
        else -> this
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

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
}
