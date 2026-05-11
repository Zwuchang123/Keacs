package com.keacs.app.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.keacs.app.data.agent.AgentActionPreview
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun AgentMessages(
    state: AgentUiState,
    settingsMessage: String?,
    onExampleClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onActionConfirm: (AgentActionPreview) -> Unit,
    onActionCancel: (AgentActionPreview) -> Unit,
    onClearConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.isSending) {
        val lastIndex = state.messages.size + if (state.isSending) 1 else 0
        if (lastIndex > 0) {
            listState.animateScrollToItem(lastIndex - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = KeacsSpacing.PageHorizontal,
            vertical = KeacsSpacing.PageVertical,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (settingsMessage != null) {
            item {
                AgentStatusCard(message = settingsMessage, onOpenSettings = onOpenSettings)
            }
        }
        if (state.messages.isNotEmpty()) {
            item {
                AgentConversationHeader(onClearConversation = onClearConversation)
            }
        }
        if (state.messages.size >= AgentViewModel.CLEANUP_WARNING_THRESHOLD) {
            item {
                LongConversationNotice(onClearConversation = onClearConversation)
            }
        }
        if (state.messages.isEmpty()) {
            item {
                AgentEmptyState(onExampleClick = onExampleClick)
            }
        }
        items(state.messages, key = { it.id }) { message ->
            AgentMessageBubble(
                message = message,
                onActionConfirm = onActionConfirm,
                onActionCancel = onActionCancel,
            )
        }
        if (state.isSending) {
            item {
                SendingBubble(startedAtMillis = state.sendingStartedAtMillis)
            }
        }
        if (state.messages.isNotEmpty() && !state.isSending) {
            item {
                AgentGuidedSuggestions(onExampleClick = onExampleClick)
            }
        }
    }
}

@Composable
private fun AgentMessageBubble(
    message: AgentMessage,
    onActionConfirm: (AgentActionPreview) -> Unit,
    onActionCancel: (AgentActionPreview) -> Unit,
) {
    val isUser = message.role == AgentMessageRole.USER
    val isError = message.role == AgentMessageRole.ERROR
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val bubbleMaxWidth = maxWidth * 0.86f
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = bubbleMaxWidth)
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        when {
                            isUser -> KeacsColors.Primary
                            isError -> KeacsColors.Error.copy(alpha = 0.1f)
                            else -> KeacsColors.Surface
                        },
                    )
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = richText(message.text),
                    color = when {
                        isUser -> KeacsColors.Surface
                        isError -> KeacsColors.Error
                        else -> KeacsColors.TextPrimary
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                message.warnings.forEach { warning ->
                    Text(
                        text = warning,
                        color = KeacsColors.Warning,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                message.actions.forEach { action ->
                    AgentActionCard(
                        action = action,
                        onConfirm = { onActionConfirm(action) },
                        onCancel = { onActionCancel(action) },
                    )
                }
                message.elapsedMillis?.let { elapsed ->
                    Text(
                        text = "回复耗时 ${elapsed.formatElapsed()}",
                        color = if (isUser) KeacsColors.Surface.copy(alpha = 0.8f) else KeacsColors.TextTertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun richText(text: String) = buildAnnotatedString {
    var index = 0
    while (index < text.length) {
        val start = text.indexOf("**", index)
        if (start < 0) {
            append(text.substring(index))
            break
        }
        append(text.substring(index, start))
        val end = text.indexOf("**", start + 2)
        if (end < 0) {
            append(text.substring(start))
            break
        }
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append(text.substring(start + 2, end))
        }
        index = end + 2
    }
}
