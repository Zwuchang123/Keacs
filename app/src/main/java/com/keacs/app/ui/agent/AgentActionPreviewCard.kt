package com.keacs.app.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.keacs.app.data.agent.AgentActionPreview
import com.keacs.app.data.agent.AgentEditOptions
import com.keacs.app.data.agent.AgentFieldOption
import com.keacs.app.data.agent.requiresConfirmation
import com.keacs.app.ui.components.formatCent
import com.keacs.app.ui.theme.KeacsColors
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun AgentActionCard(
    action: AgentActionPreview,
    editOptions: AgentEditOptions,
    onActionChange: (AgentActionPreview, String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KeacsColors.SurfaceSubtle)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
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
        EditablePreviewLines(
            action = action,
            editOptions = editOptions,
            onActionChange = onActionChange,
        )
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
private fun EditablePreviewLines(
    action: AgentActionPreview,
    editOptions: AgentEditOptions,
    onActionChange: (AgentActionPreview, String) -> Unit,
) {
    val canEditRecordFields = action.type !in setOf("delete_record")
    val canEditScheduleFields = action.type !in setOf("disable_scheduled_record")

    action.records.take(3).forEachIndexed { index, item ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val title = listOf(
                item["type"]?.toString().recordTypeLabel(),
                item["date"]?.toString() ?: item.longValue("occurredAt")?.formatDate().orEmpty(),
                item.longValue("amountCent")?.let { "¥${formatCent(it)}" }.orEmpty(),
            ).filter { it.isNotBlank() }.joinToString(" · ")
            Text(text = title.ifBlank { "账目" }, color = KeacsColors.TextPrimary, style = MaterialTheme.typography.bodySmall)

            if (canEditRecordFields) {
                EditableRecordFields(action, index, item, editOptions, onActionChange)
            } else if (action.records.size > 1) {
                TextButton(onClick = { onActionChange(action.copy(records = action.records.removeAt(index)), "records") }) {
                    Text("不处理这笔")
                }
            }
        }
    }

    action.scheduledRecords.take(3).forEachIndexed { index, item ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val title = listOf(
                item["frequency"]?.toString().frequencyLabel(),
                item.longValue("nextRunAt")?.formatDate().orEmpty(),
                item.longValue("amountCent")?.let { "¥${formatCent(it)}" }.orEmpty(),
            ).filter { it.isNotBlank() }.joinToString(" · ")
            Text(text = title.ifBlank { "定时记账" }, color = KeacsColors.TextPrimary, style = MaterialTheme.typography.bodySmall)

            if (canEditScheduleFields) {
                EditableScheduleFields(action, index, item, editOptions, onActionChange)
            } else if (action.scheduledRecords.size > 1) {
                TextButton(onClick = { onActionChange(action.copy(scheduledRecords = action.scheduledRecords.removeAt(index)), "scheduledRecords") }) {
                    Text("不处理这条")
                }
            }
        }
    }

    val extraCount = action.records.size + action.scheduledRecords.size - action.records.take(3).size - action.scheduledRecords.take(3).size
    if (extraCount > 0) {
        Text(
            text = "还有 $extraCount 项",
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun EditableRecordFields(
    action: AgentActionPreview,
    index: Int,
    item: Map<String, Any?>,
    editOptions: AgentEditOptions,
    onActionChange: (AgentActionPreview, String) -> Unit,
) {
    val type = item["type"]?.toString().orEmpty().ifBlank { "EXPENSE" }
    DropdownField(
        label = "类型",
        value = type.recordTypeLabel(),
        options = listOf(
            AgentFieldOption(null, "支出", "EXPENSE"),
            AgentFieldOption(null, "收入", "INCOME"),
            AgentFieldOption(null, "转账", "TRANSFER"),
        ),
        onSelect = { option ->
            val updatedType = option.direction
            onActionChange(action.updateRecord(index, mapOf("type" to updatedType)), "type")
        },
    )
    EditableTextField(
        label = "金额",
        value = item.longValue("amountCent")?.let { formatCent(it) }.orEmpty(),
        keyboardType = KeyboardType.Decimal,
        onValueChange = { value ->
            value.toCentOrNull()?.let { amountCent ->
                onActionChange(action.updateRecord(index, mapOf("amountCent" to amountCent)), "amountCent")
            }
        },
    )
    EditableTextField(
        label = "日期",
        value = item["date"]?.toString() ?: item.longValue("occurredAt")?.formatIsoDate().orEmpty(),
        onValueChange = { value ->
            value.toDateMillisOrNull()?.let { occurredAt ->
                onActionChange(action.updateRecord(index, mapOf("date" to value, "occurredAt" to occurredAt)), "occurredAt")
            }
        },
    )
    if (type != "TRANSFER") {
        val categoryDirection = if (type == "INCOME") "INCOME" else "EXPENSE"
        DropdownField(
            label = "分类",
            value = item["categoryName"]?.toString().orEmpty().ifBlank { "未选择" },
            options = editOptions.categories.filter { it.direction == categoryDirection },
            onSelect = { option ->
                onActionChange(
                    action.updateRecord(index, mapOf("categoryId" to option.id, "categoryName" to option.name)),
                    "category",
                )
            },
        )
    }
    if (type == "TRANSFER") {
        AccountField("转出账户", item["fromAccountName"]?.toString().orEmpty(), editOptions.accounts) { option ->
            onActionChange(action.updateRecord(index, accountUpdates("fromAccountId", "fromAccountName", option)), "fromAccount")
        }
        AccountField("转入账户", item["toAccountName"]?.toString().orEmpty(), editOptions.accounts) { option ->
            onActionChange(action.updateRecord(index, accountUpdates("toAccountId", "toAccountName", option)), "toAccount")
        }
    } else {
        val accountKey = if (type == "INCOME") "toAccountName" else "fromAccountName"
        val accountIdKey = if (type == "INCOME") "toAccountId" else "fromAccountId"
        val value = item[accountKey]?.toString() ?: item["accountName"]?.toString().orEmpty()
        AccountField("账户", value, editOptions.accounts) { option ->
            onActionChange(action.updateRecord(index, accountUpdates(accountIdKey, accountKey, option)), "account")
        }
    }
    EditableTextField(
        label = "备注",
        value = item["note"]?.toString().orEmpty(),
        onValueChange = { value -> onActionChange(action.updateRecord(index, mapOf("note" to value)), "note") },
    )
}

@Composable
private fun EditableScheduleFields(
    action: AgentActionPreview,
    index: Int,
    item: Map<String, Any?>,
    editOptions: AgentEditOptions,
    onActionChange: (AgentActionPreview, String) -> Unit,
) {
    val type = item["type"]?.toString().orEmpty().ifBlank { "EXPENSE" }
    DropdownField(
        label = "类型",
        value = type.recordTypeLabel(),
        options = listOf(
            AgentFieldOption(null, "支出", "EXPENSE"),
            AgentFieldOption(null, "收入", "INCOME"),
            AgentFieldOption(null, "转账", "TRANSFER"),
        ),
        onSelect = { option -> onActionChange(action.updateSchedule(index, mapOf("type" to option.direction)), "type") },
    )
    DropdownField(
        label = "周期",
        value = item["frequency"]?.toString().frequencyLabel(),
        options = listOf(
            AgentFieldOption(null, "每周", "WEEKLY"),
            AgentFieldOption(null, "每月", "MONTHLY"),
            AgentFieldOption(null, "每年", "YEARLY"),
        ),
        onSelect = { option -> onActionChange(action.updateSchedule(index, mapOf("frequency" to option.direction)), "frequency") },
    )
    EditableTextField(
        label = "金额",
        value = item.longValue("amountCent")?.let { formatCent(it) }.orEmpty(),
        keyboardType = KeyboardType.Decimal,
        onValueChange = { value ->
            value.toCentOrNull()?.let { amountCent ->
                onActionChange(action.updateSchedule(index, mapOf("amountCent" to amountCent)), "amountCent")
            }
        },
    )
    EditableTextField(
        label = "下次生成",
        value = item.longValue("nextRunAt")?.formatIsoDate().orEmpty(),
        onValueChange = { value ->
            value.toDateMillisOrNull()?.let { nextRunAt ->
                onActionChange(action.updateSchedule(index, mapOf("nextRunAt" to nextRunAt)), "nextRunAt")
            }
        },
    )
    if (type == "TRANSFER") {
        AccountField("转出账户", item["fromAccountName"]?.toString().orEmpty(), editOptions.accounts) { option ->
            onActionChange(action.updateSchedule(index, accountUpdates("fromAccountId", "fromAccountName", option)), "fromAccount")
        }
        AccountField("转入账户", item["toAccountName"]?.toString().orEmpty(), editOptions.accounts) { option ->
            onActionChange(action.updateSchedule(index, accountUpdates("toAccountId", "toAccountName", option)), "toAccount")
        }
    } else {
        val categoryDirection = if (type == "INCOME") "INCOME" else "EXPENSE"
        DropdownField(
            label = "分类",
            value = item["categoryName"]?.toString().orEmpty().ifBlank { "未选择" },
            options = editOptions.categories.filter { it.direction == categoryDirection },
            onSelect = { option ->
                onActionChange(
                    action.updateSchedule(index, mapOf("categoryId" to option.id, "categoryName" to option.name)),
                    "category",
                )
            },
        )
        val accountKey = if (type == "INCOME") "toAccountName" else "fromAccountName"
        val accountIdKey = if (type == "INCOME") "toAccountId" else "fromAccountId"
        AccountField("账户", item[accountKey]?.toString().orEmpty(), editOptions.accounts) { option ->
            onActionChange(action.updateSchedule(index, accountUpdates(accountIdKey, accountKey, option)), "account")
        }
    }
    DropdownField(
        label = "状态",
        value = if (item["isEnabled"] == false) "停用" else "启用",
        options = listOf(
            AgentFieldOption(null, "启用", "true"),
            AgentFieldOption(null, "停用", "false"),
        ),
        onSelect = { option ->
            onActionChange(action.updateSchedule(index, mapOf("isEnabled" to (option.direction == "true"))), "isEnabled")
        },
    )
    EditableTextField(
        label = "备注",
        value = item["note"]?.toString().orEmpty(),
        onValueChange = { value -> onActionChange(action.updateSchedule(index, mapOf("note" to value)), "note") },
    )
}

@Composable
private fun AccountField(
    label: String,
    value: String,
    accounts: List<AgentFieldOption>,
    onSelect: (AgentFieldOption?) -> Unit,
) {
    DropdownField(
        label = label,
        value = value.ifBlank { "未选择" },
        options = listOf(AgentFieldOption(null, "未选择")) + accounts,
        onSelect = { option -> onSelect(option.takeUnless { it.name == "未选择" }) },
    )
}

@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<AgentFieldOption>,
    onSelect: (AgentFieldOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text("$label：$value")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun EditableTextField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun AgentActionPreview.updateRecord(index: Int, updates: Map<String, Any?>): AgentActionPreview =
    copy(records = records.mapIndexed { itemIndex, item ->
        if (itemIndex == index) item + updates else item
    })

private fun AgentActionPreview.updateSchedule(index: Int, updates: Map<String, Any?>): AgentActionPreview =
    copy(scheduledRecords = scheduledRecords.mapIndexed { itemIndex, item ->
        if (itemIndex == index) item + updates else item
    })

private fun <T> List<T>.removeAt(index: Int): List<T> =
    filterIndexed { itemIndex, _ -> itemIndex != index }

private fun accountUpdates(idKey: String, nameKey: String, option: AgentFieldOption?): Map<String, Any?> =
    mapOf(idKey to option?.id, nameKey to option?.name, "accountName" to option?.name)

private fun Map<String, Any?>.longValue(key: String): Long? =
    when (val value = this[key]) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }

private fun String.toCentOrNull(): Long? =
    runCatching {
        BigDecimal(trim())
            .multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
            .takeIf { it > 0 }
    }.getOrNull()

private fun String.toDateMillisOrNull(): Long? =
    runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            isLenient = false
        }.parse(this)?.time
    }.getOrNull()

private fun Long.formatDate(): String =
    SimpleDateFormat("MM月dd日", Locale.getDefault()).format(Date(this))

private fun Long.formatIsoDate(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(this))

private fun String?.recordTypeLabel(): String = when (this) {
    "INCOME" -> "收入"
    "TRANSFER" -> "转账"
    "EXPENSE" -> "支出"
    else -> this.orEmpty()
}

private fun String?.frequencyLabel(): String = when (this) {
    "WEEKLY" -> "每周"
    "MONTHLY" -> "每月"
    "YEARLY" -> "每年"
    else -> this.orEmpty().ifBlank { "每月" }
}
