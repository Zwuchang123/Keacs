package com.keacs.app.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.keacs.app.data.agent.AgentActionPreview
import com.keacs.app.data.agent.AgentEditOptions
import com.keacs.app.data.agent.AgentReplySource
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
    onActionChange: (Long, AgentActionPreview, String) -> Unit,
    onFeedback: (AgentMessage, String) -> Unit,
    onClearConversation: () -> Unit,
    onToggleGuidance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.isSending) {
        val lastIndex = state.messages.size + if (state.isSending) 1 else 0
        if (lastIndex > 0) {
            listState.animateScrollToItem(lastIndex - 1)
        }
    }
    val guidanceToggleMessageId = if (!state.isGuidanceVisible && !state.isSending) {
        state.messages.lastOrNull { it.role != AgentMessageRole.USER }?.id
    } else {
        null
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
        if (state.messages.size >= AgentViewModel.CLEANUP_WARNING_THRESHOLD) {
            item {
                LongConversationNotice(onClearConversation = onClearConversation)
            }
        }
        if (state.messages.isEmpty() && state.isGuidanceVisible) {
            item {
                AgentEmptyState(
                    suggestions = state.suggestions,
                    onExampleClick = onExampleClick,
                    onToggle = onToggleGuidance,
                )
            }
        }
        items(state.messages, key = { it.id }) { message ->
            AgentMessageBubble(
                message = message,
                canRegenerate = !state.isSending && state.messages.canRegenerateMessage(message.id),
                editOptions = state.editOptions,
                onActionConfirm = onActionConfirm,
                onActionCancel = onActionCancel,
                onActionChange = onActionChange,
                onFeedback = onFeedback,
                showGuidanceToggle = message.id == guidanceToggleMessageId,
                onToggleGuidance = onToggleGuidance,
            )
        }
        if (state.isSending && state.messages.lastOrNull()?.text != "正在重新生成") {
            item {
                SendingBubble(startedAtMillis = state.sendingStartedAtMillis)
            }
        }
        if (state.messages.isNotEmpty() && !state.isSending && state.isGuidanceVisible) {
            item {
                AgentGuidedSuggestions(
                    suggestions = state.suggestions,
                    onExampleClick = onExampleClick,
                    onToggle = onToggleGuidance,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AgentMessageBubble(
    message: AgentMessage,
    canRegenerate: Boolean,
    editOptions: AgentEditOptions,
    onActionConfirm: (AgentActionPreview) -> Unit,
    onActionCancel: (AgentActionPreview) -> Unit,
    onActionChange: (Long, AgentActionPreview, String) -> Unit,
    onFeedback: (AgentMessage, String) -> Unit,
    showGuidanceToggle: Boolean,
    onToggleGuidance: () -> Unit,
) {
    val isUser = message.role == AgentMessageRole.USER
    val isError = message.role == AgentMessageRole.ERROR
    val clipboard = LocalClipboardManager.current
    var showUserCopyMenu by remember(message.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (isUser) {
                        Modifier
                            .widthIn(max = 288.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(KeacsColors.PrimaryLight)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { showUserCopyMenu = true },
                            )
                            .padding(horizontal = 15.dp, vertical = 12.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    }
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RichMessageContent(
                text = message.text,
                isUser = isUser,
                isError = isError,
            )
            message.warnings.forEach { warning ->
                AgentInfoBlock(
                    label = warning,
                    color = KeacsColors.Warning,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            message.actions.forEach { action ->
                AgentActionCard(
                    action = action,
                    editOptions = editOptions,
                    onActionChange = { updated, field -> onActionChange(message.id, updated, field) },
                    onConfirm = { onActionConfirm(action) },
                    onCancel = { onActionCancel(action) },
                )
            }
            message.elapsedMillis?.let { elapsed ->
                val sourceLabel = if (message.role == AgentMessageRole.ASSISTANT) {
                    when (message.replySource) {
                        AgentReplySource.AUTO -> "自动回复"
                        AgentReplySource.MODEL -> "大模型"
                        else -> ""
                    }
                } else {
                    ""
                }
                val elapsedText = if (sourceLabel.isBlank()) {
                    elapsed.formatElapsed()
                } else {
                    "$sourceLabel ${elapsed.formatElapsed()}"
                }
                Text(
                    text = elapsedText,
                    color = if (isUser) KeacsColors.TextSecondary else KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (message.role == AgentMessageRole.ASSISTANT) {
                AgentFeedbackRow(
                    selectedFeedback = message.feedback,
                    canRegenerate = canRegenerate,
                    showGuidanceToggle = showGuidanceToggle,
                    onLike = { onFeedback(message, AgentFeedbackLike) },
                    onDislike = { onFeedback(message, AgentFeedbackDislike) },
                    onRegenerate = { onFeedback(message, AgentFeedbackRegenerate) },
                    onCopy = { clipboard.setText(AnnotatedString(message.text)) },
                    onToggleGuidance = onToggleGuidance,
                )
            } else if (!isUser) {
                AgentCopyRow(
                    isUser = isUser,
                    onCopy = { clipboard.setText(AnnotatedString(message.text)) },
                )
            }
            if (isUser) {
                DropdownMenu(
                    expanded = showUserCopyMenu,
                    onDismissRequest = { showUserCopyMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("复制") },
                        onClick = {
                            clipboard.setText(AnnotatedString(message.text))
                            showUserCopyMenu = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RichMessageContent(
    text: String,
    isUser: Boolean,
    isError: Boolean,
) {
    val baseColor = when {
        isUser -> KeacsColors.TextPrimary
        isError -> KeacsColors.Error
        else -> KeacsColors.TextPrimary
    }
    val blocks = parseRichBlocks(text)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is AgentRichBlock.Paragraph -> Text(
                    text = richText(block.text),
                    color = baseColor,
                    style = if (isUser) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                )
                is AgentRichBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    block.items.forEach { item ->
                        Row(
                            modifier = if (isUser) Modifier else Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "•",
                                color = if (isUser) KeacsColors.TextSecondary else KeacsColors.TextSecondary,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = richText(item),
                                color = baseColor,
                                style = if (isUser) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentInfoBlock(
    label: String,
    color: Color,
    style: TextStyle,
) {
    Text(
        text = richText(label),
        color = color,
        style = style,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}

private sealed interface AgentRichBlock {
    data class Paragraph(val text: String) : AgentRichBlock
    data class Bullets(val items: List<String>) : AgentRichBlock
}

private fun parseRichBlocks(text: String): List<AgentRichBlock> {
    val blocks = mutableListOf<AgentRichBlock>()
    val bullets = mutableListOf<String>()
    val paragraph = StringBuilder()

    fun flushParagraph() {
        val value = paragraph.toString().trim()
        if (value.isNotBlank()) {
            blocks += AgentRichBlock.Paragraph(value)
        }
        paragraph.clear()
    }

    fun flushBullets() {
        if (bullets.isNotEmpty()) {
            blocks += AgentRichBlock.Bullets(bullets.toList())
            bullets.clear()
        }
    }

    text.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isBlank()) {
            flushParagraph()
            flushBullets()
            return@forEach
        }
        val bullet = line.removePrefix("- ").removePrefix("• ").takeIf { it != line }
        if (bullet != null) {
            flushParagraph()
            bullets += bullet
            return@forEach
        }
        if (paragraph.isNotEmpty()) {
            paragraph.append('\n')
        }
        paragraph.append(line)
    }
    flushParagraph()
    flushBullets()
    return blocks.ifEmpty { listOf(AgentRichBlock.Paragraph(text)) }
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
