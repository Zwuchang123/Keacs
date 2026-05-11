package com.keacs.app.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.theme.KeacsColors
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
internal fun AgentConversationHeader(onClearConversation: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "已保留原对话",
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onClearConversation) {
            Text("清空对话")
        }
    }
}

@Composable
internal fun LongConversationNotice(onClearConversation: () -> Unit) {
    KeacsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "对话有点长",
                    color = KeacsColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "可以清空后开始新问题，避免旧内容影响判断。",
                    color = KeacsColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onClearConversation) {
                Text("清空")
            }
        }
    }
}

@Composable
internal fun AgentStatusCard(
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
                )
            }
            TextButton(onClick = onOpenSettings) {
                Text("设置")
            }
        }
    }
}

@Composable
internal fun AgentGuidedSuggestions(onExampleClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "可以继续问",
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        listOf("继续分析本月支出", "帮我找最近的大额消费", "记一笔午饭 18 元").forEach { example ->
            ExampleRow(text = example, onClick = { onExampleClick(example) })
        }
    }
}

@Composable
internal fun AgentEmptyState(onExampleClick: (String) -> Unit) {
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
internal fun SendingBubble(startedAtMillis: Long?) {
    val elapsed = rememberElapsedMillis(startedAtMillis)
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
                text = if (elapsed > 0) "正在处理 ${elapsed.formatElapsed()}" else "正在处理",
                color = KeacsColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
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
private fun rememberElapsedMillis(startedAtMillis: Long?): Long {
    if (startedAtMillis == null) return 0L
    var now by remember(startedAtMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAtMillis) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    return now - startedAtMillis
}

internal fun Long.formatElapsed(): String =
    if (this < 1_000) {
        "${this}ms"
    } else {
        String.format(Locale.getDefault(), "%.1f 秒", this / 1000.0)
    }
