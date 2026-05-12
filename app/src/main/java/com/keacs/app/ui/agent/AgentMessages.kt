package com.keacs.app.ui.agent

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onActionChange: (Long, AgentActionPreview, String) -> Unit,
    onFeedback: (AgentMessage, String) -> Unit,
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
        if (state.messages.size >= AgentViewModel.CLEANUP_WARNING_THRESHOLD) {
            item {
                LongConversationNotice(onClearConversation = onClearConversation)
            }
        }
        if (state.messages.isEmpty()) {
            item {
                AgentEmptyState(suggestions = state.suggestions, onExampleClick = onExampleClick)
            }
        }
        items(state.messages, key = { it.id }) { message ->
            AgentMessageBubble(
                message = message,
                editOptions = state.editOptions,
                onActionConfirm = onActionConfirm,
                onActionCancel = onActionCancel,
                onActionChange = onActionChange,
                onFeedback = onFeedback,
            )
        }
        if (state.isSending) {
            item {
                SendingBubble(startedAtMillis = state.sendingStartedAtMillis)
            }
        }
        if (state.messages.isNotEmpty() && !state.isSending) {
            item {
                AgentGuidedSuggestions(suggestions = state.suggestions, onExampleClick = onExampleClick)
            }
        }
    }
}

@Composable
private fun AgentMessageBubble(
    message: AgentMessage,
    editOptions: AgentEditOptions,
    onActionConfirm: (AgentActionPreview) -> Unit,
    onActionCancel: (AgentActionPreview) -> Unit,
    onActionChange: (Long, AgentActionPreview, String) -> Unit,
    onFeedback: (AgentMessage, String) -> Unit,
) {
    val isUser = message.role == AgentMessageRole.USER
    val isError = message.role == AgentMessageRole.ERROR
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
                            .background(KeacsColors.TextPrimary.copy(alpha = 0.92f))
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
                Text(
                    text = elapsed.formatElapsed(),
                    color = if (isUser) KeacsColors.Surface.copy(alpha = 0.8f) else KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (message.role == AgentMessageRole.ASSISTANT) {
                AgentFeedbackRow(
                    onLike = { onFeedback(message, "like") },
                    onDislike = { onFeedback(message, "dislike") },
                    onRegenerate = { onFeedback(message, "regenerate") },
                )
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
        isUser -> KeacsColors.Surface
        isError -> KeacsColors.Error
        else -> KeacsColors.TextPrimary
    }
    val blocks = parseRichBlocks(text)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!isUser && blocks.any { it is AgentRichBlock.Info }) {
            AgentSticker(text = stickerText(text), isError = isError)
        }
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "•",
                                color = if (isUser) KeacsColors.Surface.copy(alpha = 0.82f) else KeacsColors.Primary,
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
                is AgentRichBlock.Info -> AgentInfoBlock(
                    label = block.text,
                    color = block.color,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun AgentSticker(text: String, isError: Boolean) {
    val color = if (isError) KeacsColors.Error else KeacsColors.Primary
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when {
                isError -> Icons.Rounded.ErrorOutline
                text.contains("完成") -> Icons.Rounded.CheckCircle
                else -> Icons.Rounded.Insights
            },
            contentDescription = null,
            tint = color,
            modifier = Modifier.padding(top = 1.dp),
        )
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
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
    data class Info(val text: String, val color: Color) : AgentRichBlock
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
        line.infoColor()?.let { color ->
            flushParagraph()
            flushBullets()
            blocks += AgentRichBlock.Info(line, color)
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

private fun String.infoColor(): Color? {
    val label = substringBefore("：").substringBefore(":").trim()
    return when (label) {
        "收入" -> KeacsColors.Income
        "支出" -> KeacsColors.Expense
        "结余", "范围" -> KeacsColors.Primary
        "提醒", "注意" -> KeacsColors.Warning
        "失败" -> KeacsColors.Error
        else -> null
    }
}

private fun stickerText(text: String): String =
    when {
        text.contains("已完成") -> "已完成"
        text.contains("请确认") -> "待确认"
        text.contains("失败") -> "处理失败"
        text.contains("摘要") || text.contains("复盘") -> "账本摘要"
        else -> "已整理"
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
