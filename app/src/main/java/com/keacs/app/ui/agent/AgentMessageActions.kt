package com.keacs.app.ui.agent

import com.keacs.app.data.agent.AgentActionPreview

internal const val AgentFeedbackLike = "like"
internal const val AgentFeedbackDislike = "dislike"
internal const val AgentFeedbackRegenerate = "regenerate"

internal fun toggleAgentFeedback(current: String, selected: String): String =
    if (current == selected) "" else selected

internal fun toggleAgentGuidance(current: Boolean): Boolean =
    !current

internal fun List<AgentMessage>.lastRegenerableAssistantId(): Long? =
    lastOrNull { it.role == AgentMessageRole.ASSISTANT }?.id

internal fun List<AgentMessage>.canRegenerateMessage(messageId: Long): Boolean =
    lastRegenerableAssistantId() == messageId && none { it.id == messageId && it.isStreaming }

internal fun List<AgentMessage>.withMessageFeedback(
    messageId: Long,
    feedback: String,
): List<AgentMessage> =
    map { message ->
        if (message.id == messageId && message.role == AgentMessageRole.ASSISTANT) {
            message.copy(feedback = toggleAgentFeedback(message.feedback, feedback))
        } else {
            message
        }
    }

internal fun List<AgentMessage>.replaceAssistantMessage(
    messageId: Long,
    text: String,
    actions: List<AgentActionPreview> = emptyList(),
    warnings: List<String> = emptyList(),
    elapsedMillis: Long? = null,
    isStreaming: Boolean = false,
): List<AgentMessage> =
    map { message ->
        if (message.id == messageId && message.role == AgentMessageRole.ASSISTANT) {
            message.copy(
                text = text,
                actions = actions,
                warnings = warnings,
                elapsedMillis = elapsedMillis,
                isStreaming = isStreaming,
                feedback = "",
            )
        } else {
            message
        }
    }

internal fun List<AgentMessage>.userMessageBefore(messageId: Long): AgentMessage? {
    val targetIndex = indexOfLast { it.id == messageId }
    if (targetIndex <= 0) return null
    return take(targetIndex).lastOrNull { it.role == AgentMessageRole.USER }
}
