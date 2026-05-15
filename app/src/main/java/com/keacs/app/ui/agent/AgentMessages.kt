package com.keacs.app.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.keacs.app.data.agent.AgentActionPreview
import com.keacs.app.data.agent.AgentEditOptions
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import org.commonmark.parser.Parser
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak

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
        if (isUser) {
            Box(modifier = Modifier.widthIn(max = 288.dp)) {
                Column(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .background(KeacsColors.PrimaryLight)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { showUserCopyMenu = true },
                        )
                        .padding(horizontal = 15.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RichMessageContent(
                        text = message.text,
                        isUser = true,
                        isError = false,
                    )
                    message.elapsedMillis?.let { elapsed ->
                        Text(
                            text = elapsed.formatElapsed(),
                            color = KeacsColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                DropdownMenu(
                    expanded = showUserCopyMenu,
                    onDismissRequest = { showUserCopyMenu = false },
                    containerColor = KeacsColors.Surface,
                ) {
                    DropdownMenuItem(
                        text = { Text("复制", color = KeacsColors.TextPrimary) },
                        onClick = {
                            clipboard.setText(AnnotatedString(message.text))
                            showUserCopyMenu = false
                        },
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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
                        color = KeacsColors.TextTertiary,
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
                } else {
                    AgentCopyRow(
                        isUser = isUser,
                        onCopy = { clipboard.setText(AnnotatedString(message.text)) },
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
                text = if (message.isStreaming) {
                    "正在思考${message.elapsedMillis?.let { "（已用 ${it.formatElapsed()}）" }.orEmpty()}"
                } else {
                    "已思考${message.elapsedMillis?.let { "（用时 ${it.formatElapsed()}）" }.orEmpty()}"
                },
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
    if (isUser) {
        Text(
            text = text,
            color = baseColor,
            style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        val blocks = parseRichBlocks(text)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            blocks.forEach { block ->
                when (block) {
                    is AgentRichBlock.Paragraph -> MarkdownText(
                        markdown = block.text,
                        color = baseColor,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    is AgentRichBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        block.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "•",
                                    color = KeacsColors.TextSecondary,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                MarkdownText(
                                    markdown = item,
                                    color = baseColor,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    is AgentRichBlock.Table -> AgentMarkdownTable(block)
                }
            }
        }
    }
}

@Composable
private fun AgentMarkdownTable(table: AgentRichBlock.Table) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KeacsColors.SurfaceSubtle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        table.rows.forEachIndexed { index, row ->
            if (index > 0) {
                HorizontalDivider(color = KeacsColors.Border)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                table.headers.forEachIndexed { cellIndex, header ->
                    val value = row.getOrNull(cellIndex).orEmpty()
                    if (value.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = header,
                                color = KeacsColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(0.34f),
                            )
                            MarkdownText(
                                markdown = value,
                                color = KeacsColors.TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(0.66f),
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
    MarkdownText(
        markdown = label,
        color = color,
        style = style,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}

@Composable
private fun MarkdownText(
    markdown: String,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val parser = remember { Parser.builder().build() }

    val annotatedString = remember(markdown) {
        buildAnnotatedString {
            val document = parser.parse(markdown)
            processNodes(document.firstChild, this, color, style)
        }
    }

    Text(
        text = annotatedString,
        color = color,
        style = style,
        modifier = modifier,
    )
}

private fun processNodes(node: Node?, builder: androidx.compose.ui.text.AnnotatedString.Builder, color: Color, style: TextStyle) {
    var current = node
    while (current != null) {
        when (current) {
            is Text -> builder.append(current.literal)
            is SoftLineBreak, is HardLineBreak -> builder.append("\n")
            is StrongEmphasis -> {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    processNodes(current.firstChild, builder, color, style)
                }
            }
            is Emphasis -> {
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    processNodes(current.firstChild, builder, color, style)
                }
            }
            is Code -> {
                builder.withStyle(SpanStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    background = color.copy(alpha = 0.1f),
                )) {
                    builder.append(current.literal)
                }
            }
            is Link -> {
                builder.withStyle(SpanStyle(
                    color = KeacsColors.Primary,
                    textDecoration = TextDecoration.Underline,
                )) {
                    processNodes(current.firstChild, builder, color, style)
                }
            }
            is Heading -> {
                if (builder.toAnnotatedString().isNotEmpty()) {
                    builder.append("\n")
                }
                val headingStyle = when (current.level) {
                    1 -> style.copy(fontWeight = FontWeight.Bold, fontSize = style.fontSize * 1.5f)
                    2 -> style.copy(fontWeight = FontWeight.Bold, fontSize = style.fontSize * 1.3f)
                    3 -> style.copy(fontWeight = FontWeight.Bold, fontSize = style.fontSize * 1.15f)
                    else -> style.copy(fontWeight = FontWeight.Bold)
                }
                builder.withStyle(SpanStyle(
                    fontWeight = headingStyle.fontWeight,
                    fontSize = headingStyle.fontSize,
                )) {
                    processNodes(current.firstChild, builder, color, style)
                }
                builder.append("\n")
            }
            is Paragraph -> {
                if (builder.toAnnotatedString().isNotEmpty()) {
                    builder.append("\n\n")
                }
                processNodes(current.firstChild, builder, color, style)
            }
            is BulletList -> {
                if (builder.toAnnotatedString().isNotEmpty()) {
                    builder.append("\n")
                }
                var item = current.firstChild
                while (item != null) {
                    builder.append("• ")
                    processNodes(item.firstChild, builder, color, style)
                    builder.append("\n")
                    item = item.next
                }
            }
            is OrderedList -> {
                if (builder.toAnnotatedString().isNotEmpty()) {
                    builder.append("\n")
                }
                var item = current.firstChild
                var index = 1
                while (item != null) {
                    builder.append("$index. ")
                    processNodes(item.firstChild, builder, color, style)
                    builder.append("\n")
                    item = item.next
                    index++
                }
            }
            is ThematicBreak -> {
                if (builder.toAnnotatedString().isNotEmpty()) {
                    builder.append("\n---\n")
                }
            }
            is BlockQuote -> {
                builder.append("> ")
                processNodes(current.firstChild, builder, color, style)
                builder.append("\n")
            }
            is IndentedCodeBlock -> {
                if (builder.toAnnotatedString().isNotEmpty()) {
                    builder.append("\n")
                }
                builder.withStyle(SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)) {
                    processNodes(current.firstChild, builder, color, style)
                }
                builder.append("\n")
            }
            is FencedCodeBlock -> {
                if (builder.toAnnotatedString().isNotEmpty()) {
                    builder.append("\n")
                }
                builder.withStyle(
                    SpanStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        background = color.copy(alpha = 0.08f),
                    ),
                ) {
                    builder.append(current.literal)
                }
                builder.append("\n")
            }
        }
        current = current.next
    }
}

internal sealed interface AgentRichBlock {
    data class Paragraph(val text: String) : AgentRichBlock
    data class Bullets(val items: List<String>) : AgentRichBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : AgentRichBlock
}

internal fun parseRichBlocks(text: String): List<AgentRichBlock> {
    val blocks = mutableListOf<AgentRichBlock>()
    val bullets = mutableListOf<String>()
    val paragraph = StringBuilder()
    val tableLines = mutableListOf<String>()

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

    fun flushTable() {
        if (tableLines.isNotEmpty()) {
            parseMarkdownTable(tableLines)?.let { blocks += it }
                ?: run {
                    if (paragraph.isNotEmpty()) {
                        paragraph.append('\n')
                    }
                    paragraph.append(tableLines.joinToString("\n"))
                }
            tableLines.clear()
        }
    }

    text.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isBlank()) {
            flushTable()
            flushParagraph()
            flushBullets()
            return@forEach
        }
        if (line.looksLikeTableLine()) {
            flushParagraph()
            flushBullets()
            tableLines += line
            return@forEach
        }
        flushTable()
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
    flushTable()
    flushParagraph()
    flushBullets()
    return blocks.ifEmpty { listOf(AgentRichBlock.Paragraph(text)) }
}

private fun String.looksLikeTableLine(): Boolean =
    count { it == '|' } >= 2

private fun parseMarkdownTable(lines: List<String>): AgentRichBlock.Table? {
    if (lines.size < 2 || !lines[1].isMarkdownDividerRow()) {
        return null
    }
    val headers = lines.first().toMarkdownCells()
    val rows = lines.drop(2).map { it.toMarkdownCells() }.filter { it.isNotEmpty() }
    if (headers.isEmpty() || rows.isEmpty()) {
        return null
    }
    return AgentRichBlock.Table(headers = headers, rows = rows)
}

private fun String.isMarkdownDividerRow(): Boolean =
    toMarkdownCells().all { cell ->
        cell.replace(":", "").all { it == '-' } && cell.count { it == '-' } >= 3
    }

private fun String.toMarkdownCells(): List<String> =
    trim()
        .trim('|')
        .split('|')
        .map { it.trim() }
        .filter { it.isNotBlank() }
