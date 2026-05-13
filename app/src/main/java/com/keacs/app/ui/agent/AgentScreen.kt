package com.keacs.app.ui.agent

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.keacs.app.domain.agent.validateForRequest

@Composable
fun AgentScreen(
    viewModel: AgentViewModel,
    onOpenSettings: () -> Unit,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val validation = state.settings.validateForRequest()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-agent")
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        totalDrag += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        when {
                            totalDrag <= -60f -> onSwipeLeft()
                            totalDrag >= 60f -> onSwipeRight()
                        }
                        totalDrag = 0f
                    },
                )
            },
    ) {
        AgentMessages(
            state = state,
            settingsMessage = validation.message,
            onExampleClick = viewModel::useExample,
            onOpenSettings = onOpenSettings,
            onActionConfirm = viewModel::confirmAction,
            onActionCancel = viewModel::cancelAction,
            onActionUndo = viewModel::undoActionStatus,
            onActionChange = viewModel::updateAction,
            onFeedback = viewModel::submitMessageFeedback,
            onThinkingToggle = viewModel::toggleThinking,
            onClearConversation = viewModel::clearConversation,
            onToggleGuidance = viewModel::toggleGuidance,
            modifier = Modifier.weight(1f),
        )
        AgentInputBar(
            input = state.input,
            enabled = !state.isSending,
            isSending = state.isSending,
            onInputChange = viewModel::onInputChange,
            onSend = viewModel::send,
            modifier = Modifier.imePadding(),
        )
    }
}
