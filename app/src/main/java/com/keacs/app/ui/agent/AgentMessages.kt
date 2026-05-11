package com.keacs.app.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keacs.app.data.agent.AgentActionPreview
import com.keacs.app.data.agent.requiresConfirmation
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.KeacsCard
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
                SendingBubble()
            }
        }
    }
}

@Composable
private fun AgentStatusCard(
    message: String,
    onOpenSettings: () -> Unit,
) {
    KeacsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIcon(
                icon = Icons.Rounded.ErrorOutline,
                backgroundColor = KeacsColors.Warning.copy(alpha = 0.14f),
                tint = KeacsColors.Warning,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "助手暂不可用",
                    color = KeacsColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = message,
                    color = KeacsColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onOpenSettings) {
                Text("设置")
            }
        }
    }
}

@Composable
private fun AgentEmptyState(onExampleClick: (String) -> Unit) {
    KeacsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(
                    icon = Icons.Rounded.AutoAwesome,
                    backgroundColor = KeacsColors.Primary.copy(alpha = 0.14f),
                    tint = KeacsColors.Primary,
                )
                Text(
                    text = "想记账或查账，直接输入一句话。",
                    color = KeacsColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            val examples = listOf("记一笔午饭 18 元", "这个月花了多少？", "帮我看看本月支出")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                examples.forEach { example ->
                    ExampleRow(
                        text = example,
                        onClick = { onExampleClick(example) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExampleRow(
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(KeacsColors.SurfaceSubtle)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
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
                text = message.text,
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
        }
    }
}

@Composable
private fun AgentActionCard(
    action: AgentActionPreview,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KeacsColors.SurfaceSubtle)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = action.title,
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
        )
        if (action.description.isNotBlank()) {
            Text(
                text = action.description,
                color = KeacsColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        PreviewLines(action)
        if (action.riskNotice.isNotBlank()) {
            Text(
                text = action.riskNotice,
                color = KeacsColors.Warning,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (action.requiresConfirmation()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) {
                    Text("取消")
                }
                Button(onClick = onConfirm) {
                    Text("确认")
                }
            }
        }
    }
}

@Composable
private fun PreviewLines(action: AgentActionPreview) {
    val recordLines = action.records.take(3).map { item ->
        val amount = item["amountCent"]?.toString().orEmpty()
        val category = item["categoryName"]?.toString().orEmpty()
        val account = item["accountName"]?.toString()
            ?: item["fromAccountName"]?.toString()
            ?: item["toAccountName"]?.toString()
            ?: ""
        listOf(category, account, amount).filter { it.isNotBlank() }.joinToString(" · ")
    }
    val scheduleLines = action.scheduledRecords.take(3).map { item ->
        val amount = item["amountCent"]?.toString().orEmpty()
        val frequency = item["frequency"]?.toString().orEmpty()
        val note = item["note"]?.toString().orEmpty()
        listOf(frequency, note, amount).filter { it.isNotBlank() }.joinToString(" · ")
    }
    (recordLines + scheduleLines).filter { it.isNotBlank() }.forEach { line ->
        Text(
            text = line,
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    val extraCount = action.records.size + action.scheduledRecords.size - recordLines.size - scheduleLines.size
    if (extraCount > 0) {
        Text(
            text = "还有 $extraCount 项",
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SendingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .background(KeacsColors.Surface)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = KeacsColors.Primary,
            )
            Text(
                text = "正在处理",
                color = KeacsColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
