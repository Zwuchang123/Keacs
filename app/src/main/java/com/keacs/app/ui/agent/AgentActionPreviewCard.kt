package com.keacs.app.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.keacs.app.data.agent.AgentActionPreview
import com.keacs.app.data.agent.requiresConfirmation
import com.keacs.app.ui.components.formatCent
import com.keacs.app.ui.theme.KeacsColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun AgentActionCard(
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
                    Text(if (action.type == "delete_record") "确认删除" else "确认执行")
                }
            }
        }
    }
}

@Composable
private fun PreviewLines(action: AgentActionPreview) {
    val recordLines = action.records.take(3).map { item ->
        val amount = item.longValue("amountCent")?.let { "¥${formatCent(it)}" }.orEmpty()
        val date = item["date"]?.toString()
            ?: item.longValue("occurredAt")?.formatDate()
            ?: ""
        val type = item["type"]?.toString().recordTypeLabel()
        val category = item["categoryName"]?.toString().orEmpty()
        val account = item["accountName"]?.toString()
            ?: item["fromAccountName"]?.toString()
            ?: item["toAccountName"]?.toString()
            ?: ""
        listOf(type, date, category, account, amount).filter { it.isNotBlank() }.joinToString(" · ")
    }
    val scheduleLines = action.scheduledRecords.take(3).map { item ->
        val amount = item.longValue("amountCent")?.let { "¥${formatCent(it)}" }.orEmpty()
        val frequency = item["frequency"]?.toString().frequencyLabel()
        val nextRun = item.longValue("nextRunAt")?.formatDate().orEmpty()
        val note = item["note"]?.toString().orEmpty()
        listOf(frequency, nextRun, note, amount).filter { it.isNotBlank() }.joinToString(" · ")
    }
    (recordLines + scheduleLines).filter { it.isNotBlank() }.forEach { line ->
        Text(
            text = line,
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
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

private fun Map<String, Any?>.longValue(key: String): Long? =
    when (val value = this[key]) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }

private fun Long.formatDate(): String =
    SimpleDateFormat("MM月dd日", Locale.getDefault()).format(Date(this))

private fun String?.recordTypeLabel(): String = when (this) {
    "INCOME" -> "收入"
    "TRANSFER" -> "转账"
    "EXPENSE" -> "支出"
    else -> ""
}

private fun String?.frequencyLabel(): String = when (this) {
    "WEEKLY" -> "每周"
    "MONTHLY" -> "每月"
    "YEARLY" -> "每年"
    else -> this.orEmpty()
}
