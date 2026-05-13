package com.keacs.app.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
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
    onActionUndo: (AgentActionPreview) -> Unit,
    onActionChange: (Long, AgentActionPreview, String) -> Unit,
    onFeedback: (AgentMessage, String) -> Unit,
    onThinkingToggle: (Long) -> Unit,
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
                onActionUndo = onActionUndo,
                onActionChange = onActionChange,
                onFeedback = onFeedback,
                onThinkingToggle = onThinkingToggle,
                guidanceVisible = state.isGuidanceVisible,
                onToggleGuidance = onToggleGuidance,
            )
        }
        // 流式回复期间已经有“助手消息气泡”承载输出，不再额外显示 SendingBubble，
        // 否则会让“思考块”和“最终回复”被插入一条额外气泡隔开，且削弱流式感知。
        if (state.isSending && state.messages.none { it.isStreaming } && state.messages.lastOrNull()?.text != "正在重新生成") {
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
    onActionUndo: (AgentActionPreview) -> Unit,
    onActionChange: (Long, AgentActionPreview, String) -> Unit,
    onFeedback: (AgentMessage, String) -> Unit,
    onThinkingToggle: (Long) -> Unit,
    guidanceVisible: Boolean,
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
            if (!isUser && message.thinkingSteps.isNotEmpty()) {
                AgentThinkingBlock(
                    message = message,
                    onToggle = { onThinkingToggle(message.id) },
                )
            }
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
                    onUndo = { onActionUndo(action) },
                )
            }
            message.elapsedMillis?.let { elapsed ->
                Text(
                    text = elapsed.formatElapsed(),
                    color = if (isUser) KeacsColors.TextSecondary else KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (message.role == AgentMessageRole.ASSISTANT) {
                AgentFeedbackRow(
                    selectedFeedback = message.feedback,
                    canRegenerate = canRegenerate,
                    guidanceVisible = guidanceVisible,
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
private fun AgentThinkingBlock(
    message: AgentMessage,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KeacsColors.SurfaceSubtle)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (message.isStreaming) "正在思考" else "已思考${message.elapsedMillis?.let { "（用时 ${it.formatElapsed()}）" }.orEmpty()}",
                color = KeacsColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Icon(
                imageVector = if (message.thinkingExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (message.thinkingExpanded) "收起思考" else "展开思考",
                tint = KeacsColors.TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
        if (message.thinkingExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                message.thinkingSteps.forEach { step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", color = KeacsColors.TextTertiary, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = step,
                            color = KeacsColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                    }
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
