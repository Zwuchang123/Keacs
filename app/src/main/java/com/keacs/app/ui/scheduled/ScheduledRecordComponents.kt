package com.keacs.app.ui.scheduled

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
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarToday
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
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.local.entity.ScheduledRecordEntity
import com.keacs.app.domain.model.RecordType
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.FormFieldRow
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.formatCent
import com.keacs.app.ui.management.accountIconOptionFor
import com.keacs.app.ui.management.colorFor
import com.keacs.app.ui.management.iconFor
import com.keacs.app.ui.theme.KeacsColors

@Composable
internal fun ScheduledRow(
    schedule: ScheduledRecordEntity,
    category: CategoryEntity?,
    accountNames: Map<Long, String>,
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
                text = scheduleTitle(schedule, category, accountNames),
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (schedule.isEnabled) recurrenceLabel(schedule) else "已停用 · ${recurrenceLabel(schedule)}",
                color = if (schedule.isEnabled) KeacsColors.TextSecondary else KeacsColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = signedAmountLabel(schedule),
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
internal fun EnabledField(isEnabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(KeacsColors.Surface)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = KeacsColors.TextSecondary,
            modifier = Modifier.size(21.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            "是否启用",
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = isEnabled, onCheckedChange = onEnabledChange)
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

private fun scheduleTitle(
    schedule: ScheduledRecordEntity,
    category: CategoryEntity?,
    accountNames: Map<Long, String>,
): String =
    when (schedule.type) {
        RecordType.TRANSFER -> {
            val fromName = accountNames[schedule.fromAccountId] ?: "转出账户"
            val toName = accountNames[schedule.toAccountId] ?: "转入账户"
            "$fromName 到 $toName"
        }
        else -> category?.name ?: "未选分类"
    }

private fun signedAmountLabel(schedule: ScheduledRecordEntity): String {
    val amount = formatCent(schedule.amountCent)
    return when (schedule.type) {
        RecordType.INCOME -> "+$amount"
        RecordType.EXPENSE -> "-$amount"
        else -> amount
    }
}
