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
import com.keacs.app.data.agent.AgentRepository
import com.keacs.app.data.agent.requiresConfirmation
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
    val isSending: Boolean = false,
    val lastClientRequestId: String = "",
    val sendingStartedAtMillis: Long? = null,
)

data class AgentMessage(
    val id: Long,
    val role: AgentMessageRole,
    val text: String,
    val actions: List<AgentActionPreview> = emptyList(),
    val warnings: List<String> = emptyList(),
    val elapsedMillis: Long? = null,
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()
    private var nextMessageId = 1L

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
                _uiState.update { it.copy(messages = savedMessages) }
            }
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
            when (val result = agentRepository.sendMessage(message, localContext, history)) {
                is AgentCallResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            sendingStartedAtMillis = null,
                            lastClientRequestId = result.response.clientRequestId,
                            messages = (it.messages + AgentMessage(
                                id = nextMessageId++,
                                role = AgentMessageRole.ASSISTANT,
                                text = result.response.reply,
                                actions = result.response.actions,
                                warnings = result.response.warnings,
                                elapsedMillis = System.currentTimeMillis() - startedAt,
                            )).takeLast(MAX_STORED_MESSAGES),
                        )
                    }
                    persistConversation()
                }
                is AgentCallResult.ConfigurationRequired -> keepInputAndShowError(message, result.message)
                is AgentCallResult.NetworkFailure -> keepInputAndShowError(message, result.message)
                is AgentCallResult.InvalidResponse -> keepInputAndShowError(message, result.message)
            }
        }
    }

    fun confirmAction(action: AgentActionPreview) {
        if (!action.requiresConfirmation()) return
        _uiState.update {
            it.copy(
                isSending = true,
                sendingStartedAtMillis = System.currentTimeMillis(),
                messages = it.messages.removeAction(action),
            )
        }
        persistConversation()
        val startedAt = System.currentTimeMillis()
        viewModelScope.launch {
            when (val result = actionExecutor.execute(action)) {
                is AgentExecutionResult.Success -> {
                    sendActionFeedback(action, "confirmed")
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            sendingStartedAtMillis = null,
                            messages = (it.messages + AgentMessage(
                                id = nextMessageId++,
                                role = AgentMessageRole.RESULT,
                                text = "**操作已完成**\n\n${result.message}",
                                elapsedMillis = System.currentTimeMillis() - startedAt,
                            )).takeLast(MAX_STORED_MESSAGES),
                        )
                    }
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
        _uiState.update {
            it.copy(
                messages = (it.messages.removeAction(action) + AgentMessage(
                    id = nextMessageId++,
                    role = AgentMessageRole.RESULT,
                    text = "**已取消**\n\n${action.title}",
                )).takeLast(MAX_STORED_MESSAGES),
            )
        }
        persistConversation()
        sendActionFeedback(action, "cancelled")
    }

    fun clearConversation() {
        _uiState.update {
            it.copy(input = "", messages = emptyList(), isSending = false, sendingStartedAtMillis = null)
        }
        viewModelScope.launch {
            preferencesManager.clearAgentConversationSnapshot()
        }
    }

    private fun sendActionFeedback(
        action: AgentActionPreview,
        result: String,
        errorType: String = "",
    ) {
        val clientRequestId = _uiState.value.lastClientRequestId
        if (clientRequestId.isBlank()) return
        viewModelScope.launch {
            agentRepository.sendFeedback(
                clientRequestId = clientRequestId,
                result = result,
                actionTypes = listOf(action.type),
                errorType = errorType,
            )
        }
    }

    private fun keepInputAndShowError(
        input: String,
        message: String,
        startedAt: Long? = _uiState.value.sendingStartedAtMillis,
    ) {
        _uiState.update {
            it.copy(
                input = input,
                isSending = false,
                sendingStartedAtMillis = null,
                messages = (it.messages + AgentMessage(
                    id = nextMessageId++,
                    role = AgentMessageRole.ERROR,
                    text = message,
                    elapsedMillis = startedAt?.let { start -> System.currentTimeMillis() - start },
                )).takeLast(MAX_STORED_MESSAGES),
            )
        }
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

    private fun AgentActionPreview.stableKey(): String =
        listOf(type, title, description, records.toString(), scheduledRecords.toString()).joinToString("|")

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
                contextProvider = AgentContextProvider(repository, scheduledRepository),
                actionExecutor = AgentActionExecutor(repository, scheduledRepository),
                preferencesManager = preferencesManager,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
