package com.keacs.app.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keacs.app.data.agent.AgentActionPreview
import com.keacs.app.data.agent.AgentEditOptions
import com.keacs.app.data.agent.onceActionId
import com.keacs.app.data.agent.requiresConfirmation
import com.keacs.app.ui.components.formatCent
import com.keacs.app.ui.theme.KeacsColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AgentActionCard(
    action: AgentActionPreview,
    editOptions: AgentEditOptions,
    onActionChange: (AgentActionPreview, String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onUndo: () -> Unit,
) {
    var editRequest by remember(action.onceActionId()) { mutableStateOf<AgentEditRequest?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KeacsColors.SurfaceSubtle)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(action.title, color = KeacsColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
        if (action.description.isNotBlank()) {
            Text(
                text = action.description,
                color = KeacsColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PreviewItems(
            action = action,
            editOptions = editOptions,
            onActionChange = onActionChange,
            onEditRequest = { editRequest = it },
        )
        if (action.riskNotice.isNotBlank()) {
            Text(
                text = action.riskNotice,
                color = KeacsColors.Warning,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (action.status.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (action.status == "executed") "已执行" else "已取消",
                    color = if (action.status == "executed") KeacsColors.Primary else KeacsColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                IconButton(onClick = onUndo, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = "撤销",
                        tint = KeacsColors.TextTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        } else if (action.requiresConfirmation()) {
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

    editRequest?.let { request ->
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { editRequest = null },
        ) {
            AgentEditBottomSheet(
                request = request,
                onDismiss = { editRequest = null },
            )
        }
    }
}

@Composable
private fun PreviewItems(
    action: AgentActionPreview,
    editOptions: AgentEditOptions,
    onActionChange: (AgentActionPreview, String) -> Unit,
    onEditRequest: (AgentEditRequest) -> Unit,
) {
    val items = action.previewItems()
    var selectedIndex by remember(action.onceActionId(), items.size) { mutableIntStateOf(0) }
    val safeIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    if (items.isEmpty()) {
        Text("没有可展示的明细", color = KeacsColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        return
    }

    if (items.size > 1) {
        PreviewSwitcher(
            label = items[safeIndex].pageLabel,
            canGoPrevious = safeIndex > 0,
            canGoNext = safeIndex < items.lastIndex,
            onPrevious = { selectedIndex = (safeIndex - 1).coerceAtLeast(0) },
            onNext = { selectedIndex = (safeIndex + 1).coerceAtMost(items.lastIndex) },
        )
    }
    Column(
        modifier = Modifier.pointerInput(items.size, safeIndex) {
            var totalDrag = 0f
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, dragAmount ->
                    totalDrag += dragAmount
                    change.consume()
                },
                onDragEnd = {
                    when {
                        totalDrag <= -50f -> selectedIndex = (safeIndex + 1).coerceAtMost(items.lastIndex)
                        totalDrag >= 50f -> selectedIndex = (safeIndex - 1).coerceAtLeast(0)
                    }
                    totalDrag = 0f
                },
            )
        },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PreviewItemContent(
            item = items[safeIndex],
            action = action,
            editOptions = editOptions,
            onActionChange = onActionChange,
            onEditRequest = onEditRequest,
        )
    }
}

@Composable
private fun PreviewItemContent(
    item: AgentPreviewItem,
    action: AgentActionPreview,
    editOptions: AgentEditOptions,
    onActionChange: (AgentActionPreview, String) -> Unit,
    onEditRequest: (AgentEditRequest) -> Unit,
) {
    when (item.type) {
        AgentPreviewItemType.RECORD -> {
            PreviewTitle(Icons.AutoMirrored.Rounded.ReceiptLong, recordSummary(item.data).ifBlank { "账目" })
            if (action.type == "delete_record") {
                if (action.records.size > 1) {
                    TextButton(onClick = { onActionChange(action.copy(records = action.records.removeAt(item.index)), "records") }) {
                        Text("不处理这笔")
                    }
                }
            } else {
                EditableRecordFields(action, item.index, item.data, editOptions, onActionChange, onEditRequest)
            }
        }
        AgentPreviewItemType.SCHEDULE -> {
            PreviewTitle(Icons.Rounded.EventRepeat, scheduleSummary(item.data).ifBlank { "定时记账" })
            if (action.type == "disable_scheduled_record") {
                if (action.scheduledRecords.size > 1) {
                    TextButton(onClick = {
                        onActionChange(action.copy(scheduledRecords = action.scheduledRecords.removeAt(item.index)), "scheduledRecords")
                    }) {
                        Text("不处理这条")
                    }
                }
            } else {
                EditableScheduleFields(action, item.index, item.data, editOptions, onActionChange, onEditRequest)
            }
        }
    }
}

@Composable
private fun PreviewSwitcher(
    label: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, enabled = canGoPrevious) {
            Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "上一项", modifier = Modifier.size(18.dp))
        }
        Text(label, color = KeacsColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = "下一项", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PreviewTitle(icon: ImageVector, title: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = KeacsColors.Primary, modifier = Modifier.size(20.dp))
        Text(title, color = KeacsColors.TextPrimary, style = MaterialTheme.typography.bodySmall)
    }
}

private fun recordSummary(item: Map<String, Any?>): String =
    listOf(
        item["type"]?.toString().recordTypeLabel(),
        item["date"]?.toString() ?: item.longValue("occurredAt")?.formatDate().orEmpty(),
        item.longValue("amountCent")?.let { "¥${formatCent(it)}" }.orEmpty(),
    ).filter { it.isNotBlank() }.joinToString(" · ")

private fun scheduleSummary(item: Map<String, Any?>): String =
    listOf(
        item["frequency"]?.toString().frequencyLabel(),
        item.longValue("nextRunAt")?.formatDate().orEmpty(),
        item.longValue("amountCent")?.let { "¥${formatCent(it)}" }.orEmpty(),
    ).filter { it.isNotBlank() }.joinToString(" · ")

internal fun Map<String, Any?>.longValue(key: String): Long? =
    when (val value = this[key]) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }

internal fun Long.formatDate(): String =
    SimpleDateFormat("MM月dd日", Locale.getDefault()).format(Date(this))

internal fun String?.recordTypeLabel(): String = when (this) {
    "INCOME" -> "收入"
    "TRANSFER" -> "转账"
    "EXPENSE" -> "支出"
    else -> this.orEmpty()
}

internal fun String?.frequencyLabel(): String = when (this) {
    "WEEKLY" -> "每周"
    "MONTHLY" -> "每月"
    "YEARLY" -> "每年"
    else -> this.orEmpty().ifBlank { "每月" }
}

internal fun <T> List<T>.removeAt(index: Int): List<T> =
    filterIndexed { itemIndex, _ -> itemIndex != index }
