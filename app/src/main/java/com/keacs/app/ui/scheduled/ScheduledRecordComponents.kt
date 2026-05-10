package com.keacs.app.ui.scheduled

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.local.entity.ScheduledRecordEntity
import com.keacs.app.data.repository.ScheduledFrequency
import com.keacs.app.domain.model.RecordType
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.FormFieldRow
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.management.colorFor
import com.keacs.app.ui.management.iconFor
import com.keacs.app.ui.record.dateLabel
import com.keacs.app.ui.theme.KeacsColors
import java.text.DecimalFormat

@Composable
internal fun ScheduledRow(
    schedule: ScheduledRecordEntity,
    category: CategoryEntity?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(
            icon = if (schedule.type == RecordType.TRANSFER) {
                Icons.Rounded.AccountBalanceWallet
            } else {
                iconFor(category?.iconKey ?: "more")
            },
            backgroundColor = if (schedule.type == RecordType.TRANSFER) {
                KeacsColors.Primary
            } else {
                colorFor(category?.colorKey ?: "gray")
            },
            modifier = Modifier.size(36.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (schedule.type == RecordType.TRANSFER) {
                    "转账"
                } else {
                    "${typeLabel(schedule.type)} ${category?.name ?: "未选分类"}"
                },
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${frequencyLabel(schedule.frequency)} · 下次 ${dateLabel(schedule.nextRunAt)}",
                color = if (schedule.isEnabled) KeacsColors.TextSecondary else KeacsColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = DecimalFormat("#,##0.00").format(schedule.amountCent / 100.0),
            color = when (schedule.type) {
                RecordType.INCOME -> KeacsColors.Income
                RecordType.TRANSFER -> KeacsColors.Primary
                else -> KeacsColors.Expense
            },
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun ScheduledFormArea(
    type: String,
    accounts: Map<Long, String>,
    fromAccountId: Long?,
    toAccountId: Long?,
    frequency: String,
    nextRunAt: Long,
    note: String,
    isEnabled: Boolean,
    canDelete: Boolean,
    onAccountClick: () -> Unit,
    onTimeClick: () -> Unit,
    onNoteChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    KeacsCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
        Column(Modifier.padding(it), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
            if (type != RecordType.TRANSFER) {
                FormFieldRow(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    title = "账户",
                    value = selectedAccountName(type, fromAccountId, toAccountId, accounts),
                    modifier = Modifier.clickable(onClick = onAccountClick),
                )
            }
            FormFieldRow(
                icon = Icons.Rounded.CalendarToday,
                title = "生成时间",
                value = recurrenceLabel(frequency, nextRunAt),
                modifier = Modifier.clickable(onClick = onTimeClick),
            )
            ScheduledNoteField(note = note, onNoteChange = onNoteChange)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(KeacsColors.PrimaryLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = KeacsColors.Primary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("启用", color = KeacsColors.TextPrimary, modifier = Modifier.weight(1f))
                if (canDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "删除",
                        tint = KeacsColors.Error,
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .clickable(onClick = onDelete)
                            .padding(8.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Switch(checked = isEnabled, onCheckedChange = onEnabledChange)
            }
        }
    }
}

@Composable
private fun ScheduledNoteField(note: String, onNoteChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = KeacsColors.TextSecondary)
        Spacer(modifier = Modifier.width(10.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = note,
            onValueChange = onNoteChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = KeacsColors.TextPrimary),
            cursorBrush = SolidColor(KeacsColors.Primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (note.isBlank()) {
                    Text("添加备注", color = KeacsColors.TextTertiary, style = MaterialTheme.typography.bodyMedium)
                }
                inner()
            },
        )
    }
}

internal fun scheduledValidationText(
    type: String,
    amount: Long?,
    categoryId: Long?,
    fromId: Long?,
    toId: Long?,
    error: String?,
): String? =
    error ?: when {
        amount == null -> null
        type != RecordType.TRANSFER && categoryId == null -> "请选择分类"
        type == RecordType.TRANSFER && fromId == toId -> "转出和转入账户不能相同"
        type == RecordType.TRANSFER && (fromId == null || toId == null) -> "请选择转账账户"
        else -> null
    }

private fun selectedAccountName(type: String, fromId: Long?, toId: Long?, accountNames: Map<Long, String>): String =
    if (type == RecordType.INCOME) {
        accountNames[toId] ?: "未选择"
    } else {
        accountNames[fromId] ?: "未选择"
    }

private fun typeLabel(type: String): String =
    when (type) {
        RecordType.INCOME -> "收入"
        RecordType.TRANSFER -> "转账"
        else -> "支出"
    }

private fun frequencyLabel(frequency: String): String =
    when (frequency) {
        ScheduledFrequency.DAILY -> "每天"
        ScheduledFrequency.WEEKLY -> "每周"
        ScheduledFrequency.YEARLY -> "每年"
        else -> "每月"
    }
