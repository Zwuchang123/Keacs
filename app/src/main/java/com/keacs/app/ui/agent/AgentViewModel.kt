package com.keacs.app.ui.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.keacs.app.data.agent.AgentCallResult
import com.keacs.app.data.agent.AgentContextProvider
import com.keacs.app.data.agent.AgentRepository
import com.keacs.app.data.local.PreferencesManager
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.data.repository.ScheduledRecordRepository
import com.keacs.app.domain.agent.AgentSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgentUiState(
    val settings: AgentSettings = AgentSettings(),
    val input: String = "",
    val messages: List<AgentMessage> = emptyList(),
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)

data class AgentMessage(
    val id: Long,
    val role: AgentMessageRole,
    val text: String,
    val warnings: List<String> = emptyList(),
)

enum class AgentMessageRole {
    USER,
    ASSISTANT,
    ERROR,
}

class AgentViewModel(
    private val agentRepository: AgentRepository,
    private val contextProvider: AgentContextProvider,
    preferencesManager: PreferencesManager,
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
    }

    fun onInputChange(value: String) {
        _uiState.update {
            it.copy(
                input = value.take(MAX_INPUT_LENGTH),
                errorMessage = null,
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
            _uiState.update { it.copy(errorMessage = "请输入要发送的内容。") }
            return
        }

        _uiState.update {
            it.copy(
                input = "",
                isSending = true,
                errorMessage = null,
                messages = it.messages + AgentMessage(nextMessageId++, AgentMessageRole.USER, message),
            )
        }

        viewModelScope.launch {
            val localContext = contextProvider.buildForMessage(message)
            when (val result = agentRepository.sendMessage(message, localContext)) {
                is AgentCallResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            messages = it.messages + AgentMessage(
                                id = nextMessageId++,
                                role = AgentMessageRole.ASSISTANT,
                                text = result.response.reply,
                                warnings = result.response.warnings,
                            ),
                        )
                    }
                }
                is AgentCallResult.ConfigurationRequired -> keepInputAndShowError(message, result.message)
                is AgentCallResult.NetworkFailure -> keepInputAndShowError(message, result.message)
                is AgentCallResult.InvalidResponse -> keepInputAndShowError(message, result.message)
            }
        }
    }

    private fun keepInputAndShowError(input: String, message: String) {
        _uiState.update {
            it.copy(
                input = input,
                isSending = false,
                errorMessage = message,
                messages = it.messages + AgentMessage(nextMessageId++, AgentMessageRole.ERROR, message),
            )
        }
    }

    private companion object {
        const val MAX_INPUT_LENGTH = 500
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
                preferencesManager = preferencesManager,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
