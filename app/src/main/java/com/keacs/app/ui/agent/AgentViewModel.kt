package com.keacs.app.ui.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.keacs.app.data.agent.AgentActionExecutor
import com.keacs.app.data.agent.AgentActionPreview
import com.keacs.app.data.agent.AgentCallResult
import com.keacs.app.data.agent.AgentConversationTurn
import com.keacs.app.data.agent.AgentContextProvider
import com.keacs.app.data.agent.AgentExecutionResult
import com.keacs.app.data.agent.AgentEditOptions
import com.keacs.app.data.agent.AgentRepository
import com.keacs.app.data.agent.AgentRunStore
import com.keacs.app.data.agent.requiresConfirmation
import com.keacs.app.data.agent.onceActionId
import com.keacs.app.data.local.PreferencesManager
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.data.repository.ScheduledRecordRepository
import com.keacs.app.domain.agent.AgentSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgentUiState(
    val settings: AgentSettings = AgentSettings(),
    val input: String = "",
    val messages: List<AgentMessage> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val isGuidanceVisible: Boolean = true,
    val isSending: Boolean = false,
    val lastClientRequestId: String = "",
    val sendingStartedAtMillis: Long? = null,
    val editOptions: AgentEditOptions = AgentEditOptions(),
)

data class AgentMessage(
    val id: Long,
    val role: AgentMessageRole,
    val text: String,
    val actions: List<AgentActionPreview> = emptyList(),
    val warnings: List<String> = emptyList(),
    val thinkingSteps: List<String> = emptyList(),
    val thinkingExpanded: Boolean = false,
    val isStreaming: Boolean = false,
    val elapsedMillis: Long? = null,
    val feedback: String = "",
)

enum class AgentMessageRole {
    USER,
    ASSISTANT,
    ERROR,
    RESULT,
}

class AgentViewModel(
    private val agentRepository: AgentRepository,
    private val contextProvider: AgentContextProvider,
    private val actionExecutor: AgentActionExecutor,
    private val preferencesManager: PreferencesManager,
    private val runStore: AgentRunStore = AgentRunStore(),
    private val suggestionProvider: com.keacs.app.data.agent.AgentSuggestionProvider = com.keacs.app.data.agent.AgentSuggestionProvider(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()
    private var nextMessageId = 1L
    private var suggestionSummary: Map<String, Any?> = emptyMap()

    init {
        viewModelScope.launch {
            preferencesManager.agentSettings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            val savedMessages = decodeAgentMessages(preferencesManager.agentConversationSnapshot.first())
                .takeLast(MAX_STORED_MESSAGES)
            if (savedMessages.isNotEmpty()) {
                nextMessageId = savedMessages.maxOf { it.id } + 1
                _uiState.update { it.copy(messages = savedMessages, suggestions = buildSuggestions(savedMessages)) }
            }
            refreshSuggestions()
            refreshEditOptions()
        }
    }

    fun onInputChange(value: String) {
        _uiState.update {
            it.copy(
                input = value.take(MAX_INPUT_LENGTH),
            )
        }
    }

    fun useExample(example: String) {
        onInputChange(example)
        send()
    }

    fun send() {
        val state = _uiState.value
        if (state.isSending) return
        val message = state.input.trim()
        if (message.isBlank()) {
            return
        }

        val startedAt = System.currentTimeMillis()
        val history = state.messages.toConversationTurns()
        _uiState.update {
            it.copy(
                input = "",
                isSending = true,
                sendingStartedAtMillis = startedAt,
                messages = (it.messages + AgentMessage(nextMessageId++, AgentMessageRole.USER, message))
                    .takeLast(MAX_STORED_MESSAGES),
            )
        }
        persistConversation()

        viewModelScope.launch {
            val localContext = contextProvider.buildForMessage(message)
            val assistantId = nextMessageId++
            _uiState.update { current ->
                current.copy(
                    messages = (current.messages + AgentMessage(
                        id = assistantId,
                        role = AgentMessageRole.ASSISTANT,
                        text = "",
                        thinkingSteps = listOf("正在理解"),
                        isStreaming = true,
                    )).takeLast(MAX_STORED_MESSAGES),
                )
            }
            when (val result = agentRepository.streamMessage(message, localContext, history) { event ->
                applyStreamEvent(assistantId, event, startedAt)
            }) {
                is AgentCallResult.Success -> {
                    val actions = result.response.actions.withStableActionIds(result.response.clientRequestId)
                    actions.filter { it.requiresConfirmation() }.forEach { action ->
                        runStore.savePendingAction(result.response.clientRequestId, action)
                    }
                    _uiState.update { current ->
                        val messages = current.messages.replaceAssistantMessage(
                            messageId = assistantId,
                            text = result.response.reply,
                            actions = actions,
                            warnings = result.response.warnings,
                            elapsedMillis = System.currentTimeMillis() - startedAt,
                            isStreaming = false,
                        ).takeLast(MAX_STORED_MESSAGES)
                        current.copy(
                            isSending = false,
                            sendingStartedAtMillis = null,
                            lastClientRequestId = result.response.clientRequestId,
                            messages = messages,
                            suggestions = buildSuggestions(messages),
                        )
                    }
                    refreshSuggestions(_uiState.value.messages)
                    persistConversation()
                }
                is AgentCallResult.ConfigurationRequired -> replaceStreamingMessageWithError(assistantId, message, result.message, startedAt)
                is AgentCallResult.NetworkFailure -> replaceStreamingMessageWithError(assistantId, message, result.message, startedAt)
                is AgentCallResult.InvalidResponse -> replaceStreamingMessageWithError(assistantId, message, result.message, startedAt)
            }
        }
    }

    fun confirmAction(action: AgentActionPreview) {
        if (!action.requiresConfirmation()) return
        val actionId = action.onceActionId()
        if (!runStore.markActionConfirmed(actionId)) {
            keepInputAndShowError(_uiState.value.input, "这次操作已经处理过，不能重复执行。")
            return
        }
        _uiState.update {
            it.copy(
                isSending = true,
                sendingStartedAtMillis = System.currentTimeMillis(),
            )
        }
        persistConversation()
        val startedAt = System.currentTimeMillis()
        viewModelScope.launch {
            when (val result = actionExecutor.execute(action)) {
                is AgentExecutionResult.Success -> {
                    sendActionFeedback(action, "confirmed")
                    _uiState.update { current ->
                        val messages = current.messages.replaceAction(
                            oldAction = action,
                            newAction = action.copy(
                                title = "已执行",
                                description = result.message,
                                riskNotice = "",
                                status = "executed",
                            ),
                        ).takeLast(MAX_STORED_MESSAGES)
                        current.copy(
                            isSending = false,
                            sendingStartedAtMillis = null,
                            messages = messages,
                            suggestions = buildSuggestions(messages),
                        )
                    }
                    refreshSuggestions(_uiState.value.messages)
                    persistConversation()
                }
                is AgentExecutionResult.Failure -> {
                    sendActionFeedback(action, "failed", "local_execution_failed")
                    keepInputAndShowError(_uiState.value.input, result.message, startedAt)
                }
            }
        }
    }

    fun cancelAction(action: AgentActionPreview) {
        runStore.markActionCancelled(action.onceActionId())
        _uiState.update { current ->
            val messages = current.messages.replaceAction(
                oldAction = action,
                newAction = action.copy(
                    title = "已取消",
                    description = action.title,
                    riskNotice = "",
                    status = "cancelled",
                ),
            ).takeLast(MAX_STORED_MESSAGES)
            current.copy(
                messages = messages,
                suggestions = buildSuggestions(messages),
            )
        }
        persistConversation()
        sendActionFeedback(action, "cancelled")
    }

    fun undoActionStatus(action: AgentActionPreview) {
        if (action.status == "cancelled") {
            val restored = action.copy(status = "", title = action.description.ifBlank { action.title })
            runStore.savePendingAction(_uiState.value.lastClientRequestId, restored)
            _uiState.update { current ->
                current.copy(messages = current.messages.replaceAction(action, restored))
            }
            persistConversation()
            return
        }
        keepInputAndShowError(_uiState.value.input, "已执行的操作暂不能自动撤销，可以在账目详情中修改。")
    }

    fun clearConversation() {
        _uiState.update {
            it.copy(
                input = "",
                messages = emptyList(),
                suggestions = buildSuggestions(emptyList()),
                isGuidanceVisible = true,
                isSending = false,
                sendingStartedAtMillis = null,
            )
        }
        refreshSuggestions(emptyList())
        viewModelScope.launch {
            preferencesManager.clearAgentConversationSnapshot()
        }
    }

    fun toggleGuidance() {
        _uiState.update {
            it.copy(
                isGuidanceVisible = nextAgentGuidanceVisibility(
                    current = it.isGuidanceVisible,
                    hasMessages = it.messages.isNotEmpty(),
                ),
            )
        }
    }

    fun updateAction(messageId: Long, action: AgentActionPreview, changedField: String) {
        _uiState.update { current ->
            current.copy(
                messages = current.messages.map { message ->
                    if (message.id != messageId) {
                        message
                    } else {
                        message.copy(
                            actions = message.actions.map { existing ->
                                if (existing.onceActionId() == action.onceActionId()) action else existing
                            },
                        )
                    }
                },
            )
        }
        runStore.updatePendingAction(action)
        persistConversation()
        sendActionFeedback(action, "edited", reason = changedField)
    }

    fun toggleThinking(messageId: Long) {
        _uiState.update { current ->
            current.copy(
                messages = current.messages.map { message ->
                    if (message.id == messageId) {
                        message.copy(thinkingExpanded = !message.thinkingExpanded)
                    } else {
                        message
                    }
                },
            )
        }
        persistConversation()
    }

    fun submitMessageFeedback(message: AgentMessage, feedback: String) {
        if (message.role != AgentMessageRole.ASSISTANT) return
        if (feedback == AgentFeedbackRegenerate) {
            regenerateMessage(message)
            return
        }
        _uiState.update { current ->
            current.copy(messages = current.messages.withMessageFeedback(message.id, feedback))
        }
        persistConversation()
        val clientRequestId = _uiState.value.lastClientRequestId
        if (clientRequestId.isBlank()) return
        viewModelScope.launch {
            agentRepository.sendFeedback(
                clientRequestId = clientRequestId,
                result = feedback,
                actionTypes = emptyList(),
            )
        }
    }

    private fun regenerateMessage(message: AgentMessage) {
        val state = _uiState.value
        if (state.isSending || !state.messages.canRegenerateMessage(message.id)) return
        val userMessage = state.messages.userMessageBefore(message.id) ?: return
        val startedAt = System.currentTimeMillis()
        val history = state.messages
            .takeWhile { it.id != message.id }
            .toConversationTurns()
            .dropLast(1)
        _uiState.update {
            it.copy(
                isSending = true,
                sendingStartedAtMillis = startedAt,
                messages = it.messages.replaceAssistantMessage(
                    messageId = message.id,
                    text = "正在重新生成",
                ),
            )
        }
        persistConversation()
        viewModelScope.launch {
            val localContext = contextProvider.buildForMessage(userMessage.text)
            when (val result = agentRepository.sendMessage(userMessage.text, localContext, history)) {
                is AgentCallResult.Success -> {
                    val actions = result.response.actions.withStableActionIds(result.response.clientRequestId)
                    actions.filter { it.requiresConfirmation() }.forEach { action ->
                        runStore.savePendingAction(result.response.clientRequestId, action)
                    }
                    _uiState.update { current ->
                        val messages = current.messages.replaceAssistantMessage(
                            messageId = message.id,
                            text = result.response.reply,
                            actions = actions,
                            warnings = result.response.warnings,
                            elapsedMillis = System.currentTimeMillis() - startedAt,
                        )
                        current.copy(
                            isSending = false,
                            sendingStartedAtMillis = null,
                            lastClientRequestId = result.response.clientRequestId,
                            messages = messages,
                            suggestions = buildSuggestions(messages),
                        )
                    }
                    refreshSuggestions(_uiState.value.messages)
                    persistConversation()
                }
                is AgentCallResult.ConfigurationRequired -> replaceRegeneratedMessageWithError(message.id, result.message, startedAt)
                is AgentCallResult.NetworkFailure -> replaceRegeneratedMessageWithError(message.id, result.message, startedAt)
                is AgentCallResult.InvalidResponse -> replaceRegeneratedMessageWithError(message.id, result.message, startedAt)
            }
        }
    }

    private fun replaceRegeneratedMessageWithError(
        messageId: Long,
        error: String,
        startedAt: Long,
    ) {
        _uiState.update { current ->
            current.copy(
                isSending = false,
                sendingStartedAtMillis = null,
                messages = current.messages.replaceAssistantMessage(
                    messageId = messageId,
                    text = error,
                    elapsedMillis = System.currentTimeMillis() - startedAt,
                ),
            )
        }
        persistConversation()
    }

    private fun replaceStreamingMessageWithError(
        messageId: Long,
        input: String,
        error: String,
        startedAt: Long,
    ) {
        _uiState.update { current ->
            current.copy(
                input = input,
                isSending = false,
                sendingStartedAtMillis = null,
                messages = current.messages.map { message ->
                    if (message.id == messageId) {
                        message.copy(
                            role = AgentMessageRole.ERROR,
                            text = error,
                            isStreaming = false,
                            elapsedMillis = System.currentTimeMillis() - startedAt,
                        )
                    } else {
                        message
                    }
                },
            )
        }
        persistConversation()
    }

    private fun applyStreamEvent(
        messageId: Long,
        event: com.keacs.app.data.agent.AgentRunEvent,
        startedAt: Long,
    ) {
        _uiState.update { current ->
            current.copy(
                messages = current.messages.map { message ->
                    if (message.id != messageId) {
                        message
                    } else {
                        when (event) {
                            is com.keacs.app.data.agent.AgentRunEvent.StageChanged -> {
                                message.copy(
                                    thinkingSteps = (message.thinkingSteps + event.stage.label).distinct(),
                                    elapsedMillis = System.currentTimeMillis() - startedAt,
                                )
                            }
                            is com.keacs.app.data.agent.AgentRunEvent.ContextRequested -> {
                                val text = event.requests.joinToString("，") { it.reason.ifBlank { it.type } }
                                message.copy(
                                    thinkingSteps = (message.thinkingSteps + text).filter { it.isNotBlank() }.distinct(),
                                    elapsedMillis = System.currentTimeMillis() - startedAt,
                                )
                            }
                            is com.keacs.app.data.agent.AgentRunEvent.ThinkingStep -> {
                                message.copy(
                                    thinkingSteps = (message.thinkingSteps + event.content)
                                        .filter { it.isNotBlank() }
                                        .takeLast(24),
                                    elapsedMillis = System.currentTimeMillis() - startedAt,
                                )
                            }
                            is com.keacs.app.data.agent.AgentRunEvent.PartialMessage -> {
                                message.copy(text = message.text + event.content)
                            }
                            is com.keacs.app.data.agent.AgentRunEvent.ActionPreview -> {
                                message.copy(actions = event.actions)
                            }
                            is com.keacs.app.data.agent.AgentRunEvent.FinalMessage -> {
                                message.copy(
                                    text = event.reply,
                                    warnings = event.warnings,
                                    isStreaming = false,
                                    elapsedMillis = System.currentTimeMillis() - startedAt,
                                )
                            }
                            is com.keacs.app.data.agent.AgentRunEvent.RunFailed -> {
                                message.copy(
                                    role = AgentMessageRole.ERROR,
                                    text = event.message,
                                    isStreaming = false,
                                    elapsedMillis = System.currentTimeMillis() - startedAt,
                                )
                            }
                            else -> message
                        }
                    }
                },
            )
        }
    }

    private fun sendActionFeedback(
        action: AgentActionPreview,
        result: String,
        errorType: String = "",
        reason: String = "",
    ) {
        val clientRequestId = _uiState.value.lastClientRequestId
        if (clientRequestId.isBlank()) return
        viewModelScope.launch {
            agentRepository.sendFeedback(
                clientRequestId = clientRequestId,
                result = result,
                actionTypes = listOf(action.type),
                errorType = errorType,
                reason = reason,
            )
        }
    }

    private fun keepInputAndShowError(
        input: String,
        message: String,
        startedAt: Long? = _uiState.value.sendingStartedAtMillis,
    ) {
        _uiState.update { current ->
            val messages = (current.messages + AgentMessage(
                id = nextMessageId++,
                role = AgentMessageRole.ERROR,
                text = message,
                elapsedMillis = startedAt?.let { start -> System.currentTimeMillis() - start },
            )).takeLast(MAX_STORED_MESSAGES)
            current.copy(
                input = input,
                isSending = false,
                sendingStartedAtMillis = null,
                messages = messages,
                suggestions = buildSuggestions(messages),
            )
        }
        refreshSuggestions(_uiState.value.messages)
        persistConversation()
    }

    private fun persistConversation() {
        val snapshot = encodeAgentMessages(_uiState.value.messages.takeLast(MAX_STORED_MESSAGES))
        viewModelScope.launch {
            preferencesManager.setAgentConversationSnapshot(snapshot)
        }
    }

    private fun List<AgentMessage>.toConversationTurns(): List<AgentConversationTurn> =
        filter { it.role == AgentMessageRole.USER || it.role == AgentMessageRole.ASSISTANT }
            .map {
                AgentConversationTurn(
                    role = if (it.role == AgentMessageRole.ASSISTANT) "assistant" else "user",
                    content = it.text,
                )
            }

    private fun List<AgentMessage>.removeAction(action: AgentActionPreview): List<AgentMessage> {
        val key = action.stableKey()
        return map { message ->
            message.copy(actions = message.actions.filterNot { it.stableKey() == key })
        }
    }

    private fun List<AgentMessage>.replaceAction(
        oldAction: AgentActionPreview,
        newAction: AgentActionPreview,
    ): List<AgentMessage> {
        val key = oldAction.stableKey()
        return map { message ->
            message.copy(
                actions = message.actions.map { existing ->
                    if (existing.stableKey() == key) newAction else existing
                },
            )
        }
    }

    private fun AgentActionPreview.stableKey(): String =
        onceActionId()

    private fun List<AgentActionPreview>.withStableActionIds(clientRequestId: String): List<AgentActionPreview> =
        mapIndexed { index, action ->
            if (!action.requiresConfirmation() || action.actionId.isNotBlank()) {
                action
            } else {
                action.copy(actionId = "$clientRequestId-$index")
            }
        }

    private fun buildSuggestions(messages: List<AgentMessage>): List<String> {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return suggestionProvider.buildLocalSuggestions(
            today = today,
            recentMessages = messages.takeLast(8).map { it.text },
            localSummary = suggestionSummary,
        ).map { it.text }
    }

    private fun refreshSuggestions(messages: List<AgentMessage> = _uiState.value.messages) {
        viewModelScope.launch {
            suggestionSummary = contextProvider.buildSuggestionSummary()
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val suggestions = agentRepository.loadSuggestions(
                today = today,
                recentHistory = messages.toConversationTurns().takeLast(8),
                localSummary = suggestionSummary,
            ).map { it.text }
            _uiState.update { current ->
                if (current.messages.size == messages.size) {
                    current.copy(suggestions = suggestions)
                } else {
                    current
                }
            }
        }
    }

    private fun refreshEditOptions() {
        viewModelScope.launch {
            _uiState.update { it.copy(editOptions = contextProvider.buildEditOptions()) }
        }
    }

    companion object {
        const val CLEANUP_WARNING_THRESHOLD = 60
        const val MAX_INPUT_LENGTH = 1200
        private const val MAX_STORED_MESSAGES = 80
    }
}

class AgentViewModelFactory(
    private val preferencesManager: PreferencesManager,
    private val repository: LocalDataRepository,
    private val scheduledRepository: ScheduledRecordRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgentViewModel::class.java)) {
            return AgentViewModel(
                agentRepository = AgentRepository(preferencesManager),
                contextProvider = AgentContextProvider(
                    repository = repository,
                    scheduledRepository = scheduledRepository,
                    defaultRecordAccountIdProvider = { preferencesManager.defaultRecordAccountId.first() },
                ),
                actionExecutor = AgentActionExecutor(repository, scheduledRepository),
                preferencesManager = preferencesManager,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
