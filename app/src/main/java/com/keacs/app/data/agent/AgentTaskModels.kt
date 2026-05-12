package com.keacs.app.data.agent

enum class AgentRunStage(val label: String) {
    UNDERSTANDING("正在理解"),
    READING_CONTEXT("正在读取账本"),
    REASONING("正在生成结果"),
    VALIDATING("正在校验"),
    AWAITING_CONFIRMATION("等待确认"),
    FINALIZING("正在收尾"),
    COMPLETED("已完成"),
    FAILED("处理失败"),
}

data class AgentRun(
    val runId: String,
    val clientRequestId: String,
    val stage: AgentRunStage = AgentRunStage.UNDERSTANDING,
    val pendingActionIds: List<String> = emptyList(),
    val failureReason: String = "",
)

sealed interface AgentRunEvent {
    data class RunStarted(val runId: String) : AgentRunEvent
    data class StageChanged(val stage: AgentRunStage) : AgentRunEvent
    data class ContextRequested(
        val runId: String,
        val requests: List<AgentContextRequest>,
    ) : AgentRunEvent
    data class PartialMessage(val content: String) : AgentRunEvent
    data class ActionPreview(
        val runId: String,
        val actions: List<AgentActionPreview>,
    ) : AgentRunEvent
    data class AwaitingConfirmation(
        val runId: String,
        val actionIds: List<String>,
    ) : AgentRunEvent
    data class FinalMessage(
        val reply: String,
        val warnings: List<String> = emptyList(),
    ) : AgentRunEvent
    data class RunFailed(
        val message: String,
        val retryable: Boolean,
    ) : AgentRunEvent
}

data class AgentRunViewState(
    val runId: String = "",
    val stage: AgentRunStage = AgentRunStage.UNDERSTANDING,
    val partialMessage: String = "",
    val finalMessage: String = "",
    val contextNotice: String = "",
    val pendingActions: List<AgentActionPreview> = emptyList(),
    val warnings: List<String> = emptyList(),
    val failedMessage: String = "",
)

object AgentEventReducer {
    fun reduceAll(initial: AgentRunViewState, events: List<AgentRunEvent>): AgentRunViewState =
        events.fold(initial, ::reduce)

    fun reduce(state: AgentRunViewState, event: AgentRunEvent): AgentRunViewState =
        when (event) {
            is AgentRunEvent.RunStarted -> state.copy(runId = event.runId)
            is AgentRunEvent.StageChanged -> state.copy(stage = event.stage)
            is AgentRunEvent.ContextRequested -> state.copy(
                runId = event.runId,
                stage = AgentRunStage.READING_CONTEXT,
                contextNotice = event.requests.joinToString("，") { it.reason.ifBlank { it.type } },
            )
            is AgentRunEvent.PartialMessage -> state.copy(partialMessage = state.partialMessage + event.content)
            is AgentRunEvent.ActionPreview -> state.copy(
                runId = event.runId,
                pendingActions = event.actions,
            )
            is AgentRunEvent.AwaitingConfirmation -> state.copy(
                runId = event.runId,
                stage = AgentRunStage.AWAITING_CONFIRMATION,
                pendingActions = state.pendingActions.filter { action ->
                    action.actionId.isBlank() || action.actionId in event.actionIds
                },
            )
            is AgentRunEvent.FinalMessage -> state.copy(
                stage = AgentRunStage.COMPLETED,
                finalMessage = event.reply,
                warnings = event.warnings,
            )
            is AgentRunEvent.RunFailed -> state.copy(
                stage = AgentRunStage.FAILED,
                failedMessage = event.message,
            )
        }
}

class AgentRunStore {
    private val pendingActions = linkedMapOf<String, StoredPendingAction>()
    private val consumedActionIds = mutableSetOf<String>()

    fun savePendingAction(runId: String, action: AgentActionPreview) {
        val actionId = action.onceActionId()
        pendingActions[actionId] = StoredPendingAction(runId, action.copy(actionId = actionId))
    }

    fun pendingActions(): List<AgentActionPreview> =
        pendingActions.values.map { it.action }

    fun markActionConfirmed(actionId: String): Boolean =
        consume(actionId)

    fun markActionCancelled(actionId: String): Boolean =
        consume(actionId)

    private fun consume(actionId: String): Boolean {
        if (actionId.isBlank() || actionId in consumedActionIds) {
            return false
        }
        val removed = pendingActions.remove(actionId) ?: return false
        consumedActionIds += removed.action.actionId
        return true
    }

    private data class StoredPendingAction(
        val runId: String,
        val action: AgentActionPreview,
    )
}

data class AgentFeedbackEvent(
    val runId: String,
    val messageId: String,
    val feedback: String,
    val createdAt: Long,
)

data class AgentUserMemory(
    val commonCategories: List<String> = emptyList(),
    val commonAccounts: List<String> = emptyList(),
    val expressions: List<String> = emptyList(),
    val answerPreference: String = "",
)

data class AgentSuggestion(
    val text: String,
    val reason: String,
)

class AgentSuggestionProvider {
    fun buildLocalSuggestions(
        today: String,
        recentMessages: List<String>,
        localSummary: Map<String, Any?>,
        limit: Int = 4,
    ): List<AgentSuggestion> {
        val candidates = mutableListOf<AgentSuggestion>()
        if (today.endsWith("-28") || today.endsWith("-29") || today.endsWith("-30") || today.endsWith("-31")) {
            candidates += AgentSuggestion("月末复盘本月支出", "month_end")
        }
        if (today.endsWith("-01")) {
            candidates += AgentSuggestion("看看上月结余", "month_start")
        }
        val recentText = recentMessages.joinToString(" ")
        if ("餐饮" in recentText) {
            candidates += AgentSuggestion("继续看本月餐饮明细", "recent_topic")
        }
        if (localSummary["hasLargeExpense"] == true || localSummary["largeExpense"] == true) {
            candidates += AgentSuggestion("找出最近的大额消费", "spending_change")
        }
        candidates += AgentSuggestion("记一笔今天的支出", "user_habit")
        candidates += AgentSuggestion("分析最近7天消费", "date")
        return candidates.distinctBy { it.text }.take(limit.coerceIn(2, 4))
    }
}

fun AgentActionPreview.onceActionId(): String =
    actionId.ifBlank {
        listOf(type, title, description, records.toString(), scheduledRecords.toString()).joinToString("|")
    }
