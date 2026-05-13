package com.keacs.app.ui.agent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.keacs.app.data.agent.AgentActionPreview
import com.keacs.app.data.agent.AgentEditOptions
import com.keacs.app.data.agent.AgentFieldOption
import com.keacs.app.ui.components.FormFieldRow
import com.keacs.app.ui.components.formatCent
import com.keacs.app.ui.theme.KeacsColors
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Locale

internal data class AgentEditRequest(
    val label: String,
    val value: String,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val options: List<AgentFieldOption> = emptyList(),
    val onValueChange: ((String) -> Unit)? = null,
    val onSelect: ((AgentFieldOption) -> Unit)? = null,
)

@Composable
internal fun EditableRecordFields(
    action: AgentActionPreview,
    index: Int,
    item: Map<String, Any?>,
    editOptions: AgentEditOptions,
    onActionChange: (AgentActionPreview, String) -> Unit,
    onEditRequest: (AgentEditRequest) -> Unit,
) {
    val type = item["type"]?.toString().orEmpty().ifBlank { "EXPENSE" }
    EditableOptionRow(Icons.Rounded.SyncAlt, "类型", type.recordTypeLabel(), recordTypeOptions, onEditRequest) {
        onActionChange(action.updateRecord(index, mapOf("type" to it.direction)), "type")
    }
    EditableInputRow(Icons.Rounded.Payments, "金额", item.longValue("amountCent")?.let { formatCent(it) }.orEmpty(), KeyboardType.Decimal, onEditRequest) {
        it.toCentOrNull()?.let { amountCent -> onActionChange(action.updateRecord(index, mapOf("amountCent" to amountCent)), "amountCent") }
    }
    EditableInputRow(Icons.Rounded.CalendarToday, "日期", item["date"]?.toString() ?: item.longValue("occurredAt")?.formatIsoDate().orEmpty(), KeyboardType.Text, onEditRequest) {
        it.toDateMillisOrNull()?.let { occurredAt -> onActionChange(action.updateRecord(index, mapOf("date" to it, "occurredAt" to occurredAt)), "occurredAt") }
    }
    if (type != "TRANSFER") {
        val direction = if (type == "INCOME") "INCOME" else "EXPENSE"
        EditableOptionRow(Icons.Rounded.Category, "分类", item["categoryName"]?.toString().orEmpty().ifBlank { "未选择" }, editOptions.categories.filter { it.direction == direction }, onEditRequest) {
            onActionChange(action.updateRecord(index, mapOf("categoryId" to it.id, "categoryName" to it.name)), "category")
        }
    }
    if (type == "TRANSFER") {
        AccountField("转出账户", item["fromAccountName"]?.toString().orEmpty(), editOptions.accounts, onEditRequest) {
            onActionChange(action.updateRecord(index, accountUpdates("fromAccountId", "fromAccountName", it)), "fromAccount")
        }
        AccountField("转入账户", item["toAccountName"]?.toString().orEmpty(), editOptions.accounts, onEditRequest) {
            onActionChange(action.updateRecord(index, accountUpdates("toAccountId", "toAccountName", it)), "toAccount")
        }
    } else {
        val accountKey = if (type == "INCOME") "toAccountName" else "fromAccountName"
        val accountIdKey = if (type == "INCOME") "toAccountId" else "fromAccountId"
        val value = item[accountKey]?.toString() ?: item["accountName"]?.toString().orEmpty()
        AccountField("账户", value, editOptions.accounts, onEditRequest) {
            onActionChange(action.updateRecord(index, accountUpdates(accountIdKey, accountKey, it)), "account")
        }
    }
    EditableInputRow(Icons.AutoMirrored.Rounded.Notes, "备注", item["note"]?.toString().orEmpty(), KeyboardType.Text, onEditRequest) {
        onActionChange(action.updateRecord(index, mapOf("note" to it)), "note")
    }
}

@Composable
internal fun EditableScheduleFields(
    action: AgentActionPreview,
    index: Int,
    item: Map<String, Any?>,
    editOptions: AgentEditOptions,
    onActionChange: (AgentActionPreview, String) -> Unit,
    onEditRequest: (AgentEditRequest) -> Unit,
) {
    val type = item["type"]?.toString().orEmpty().ifBlank { "EXPENSE" }
    EditableOptionRow(Icons.Rounded.SyncAlt, "类型", type.recordTypeLabel(), recordTypeOptions, onEditRequest) {
        onActionChange(action.updateSchedule(index, mapOf("type" to it.direction)), "type")
    }
    EditableOptionRow(Icons.Rounded.EventRepeat, "周期", item["frequency"]?.toString().frequencyLabel(), frequencyOptions, onEditRequest) {
        onActionChange(action.updateSchedule(index, mapOf("frequency" to it.direction)), "frequency")
    }
    EditableInputRow(Icons.Rounded.Payments, "金额", item.longValue("amountCent")?.let { formatCent(it) }.orEmpty(), KeyboardType.Decimal, onEditRequest) {
        it.toCentOrNull()?.let { amountCent -> onActionChange(action.updateSchedule(index, mapOf("amountCent" to amountCent)), "amountCent") }
    }
    EditableInputRow(Icons.Rounded.CalendarToday, "下次生成", item.longValue("nextRunAt")?.formatIsoDate().orEmpty(), KeyboardType.Text, onEditRequest) {
        it.toDateMillisOrNull()?.let { nextRunAt -> onActionChange(action.updateSchedule(index, mapOf("nextRunAt" to nextRunAt)), "nextRunAt") }
    }
    if (type == "TRANSFER") {
        AccountField("转出账户", item["fromAccountName"]?.toString().orEmpty(), editOptions.accounts, onEditRequest) {
            onActionChange(action.updateSchedule(index, accountUpdates("fromAccountId", "fromAccountName", it)), "fromAccount")
        }
        AccountField("转入账户", item["toAccountName"]?.toString().orEmpty(), editOptions.accounts, onEditRequest) {
            onActionChange(action.updateSchedule(index, accountUpdates("toAccountId", "toAccountName", it)), "toAccount")
        }
    } else {
        val direction = if (type == "INCOME") "INCOME" else "EXPENSE"
        EditableOptionRow(Icons.Rounded.Category, "分类", item["categoryName"]?.toString().orEmpty().ifBlank { "未选择" }, editOptions.categories.filter { it.direction == direction }, onEditRequest) {
            onActionChange(action.updateSchedule(index, mapOf("categoryId" to it.id, "categoryName" to it.name)), "category")
        }
        val accountKey = if (type == "INCOME") "toAccountName" else "fromAccountName"
        val accountIdKey = if (type == "INCOME") "toAccountId" else "fromAccountId"
        AccountField("账户", item[accountKey]?.toString().orEmpty(), editOptions.accounts, onEditRequest) {
            onActionChange(action.updateSchedule(index, accountUpdates(accountIdKey, accountKey, it)), "account")
        }
    }
    EditableOptionRow(Icons.Rounded.ToggleOn, "状态", if (item["isEnabled"] == false) "停用" else "启用", enabledOptions, onEditRequest) {
        onActionChange(action.updateSchedule(index, mapOf("isEnabled" to (it.direction == "true"))), "isEnabled")
    }
    EditableInputRow(Icons.AutoMirrored.Rounded.Notes, "备注", item["note"]?.toString().orEmpty(), KeyboardType.Text, onEditRequest) {
        onActionChange(action.updateSchedule(index, mapOf("note" to it)), "note")
    }
}

@Composable
internal fun AgentEditBottomSheet(request: AgentEditRequest, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("修改${request.label}", color = KeacsColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
        if (request.options.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(request.options) { option ->
                    TextButton(
                        onClick = {
                            request.onSelect?.invoke(option)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(option.name)
                    }
                }
            }
        } else {
            var text by remember(request.label, request.value) { mutableStateOf(request.value) }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(request.label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = request.keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    request.onValueChange?.invoke(text)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("保存")
            }
        }
    }
}

@Composable
private fun AccountField(
    label: String,
    value: String,
    accounts: List<AgentFieldOption>,
    onEditRequest: (AgentEditRequest) -> Unit,
    onSelect: (AgentFieldOption?) -> Unit,
) {
    EditableOptionRow(
        icon = Icons.Rounded.AccountBalanceWallet,
        label = label,
        value = value.ifBlank { "未选择" },
        options = listOf(AgentFieldOption(null, "未选择")) + accounts,
        onEditRequest = onEditRequest,
        onSelect = { onSelect(it.takeUnless { option -> option.name == "未选择" }) },
    )
}

@Composable
private fun EditableOptionRow(
    icon: ImageVector,
    label: String,
    value: String,
    options: List<AgentFieldOption>,
    onEditRequest: (AgentEditRequest) -> Unit,
    onSelect: (AgentFieldOption) -> Unit,
) {
    EditableSummaryRow(icon, label, value) {
        onEditRequest(AgentEditRequest(label = label, value = value, options = options, onSelect = onSelect))
    }
}

@Composable
private fun EditableInputRow(
    icon: ImageVector,
    label: String,
    value: String,
    keyboardType: KeyboardType,
    onEditRequest: (AgentEditRequest) -> Unit,
    onValueChange: (String) -> Unit,
) {
    EditableSummaryRow(icon, label, value) {
        onEditRequest(AgentEditRequest(label = label, value = value, keyboardType = keyboardType, onValueChange = onValueChange))
    }
}

@Composable
private fun EditableSummaryRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    FormFieldRow(
        icon = icon,
        title = label,
        value = value.ifBlank { "未填写" },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private fun AgentActionPreview.updateRecord(index: Int, updates: Map<String, Any?>): AgentActionPreview =
    copy(records = records.mapIndexed { itemIndex, item -> if (itemIndex == index) item + updates else item })

private fun AgentActionPreview.updateSchedule(index: Int, updates: Map<String, Any?>): AgentActionPreview =
    copy(scheduledRecords = scheduledRecords.mapIndexed { itemIndex, item -> if (itemIndex == index) item + updates else item })

private fun accountUpdates(idKey: String, nameKey: String, option: AgentFieldOption?): Map<String, Any?> =
    mapOf(idKey to option?.id, nameKey to option?.name, "accountName" to option?.name)

private fun String.toCentOrNull(): Long? =
    runCatching {
        BigDecimal(trim()).multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong().takeIf { it > 0 }
    }.getOrNull()

private fun String.toDateMillisOrNull(): Long? =
    runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { isLenient = false }.parse(this)?.time
    }.getOrNull()

private fun Long.formatIsoDate(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(this)

private val recordTypeOptions = listOf(
    AgentFieldOption(null, "支出", "EXPENSE"),
    AgentFieldOption(null, "收入", "INCOME"),
    AgentFieldOption(null, "转账", "TRANSFER"),
)

private val frequencyOptions = listOf(
    AgentFieldOption(null, "每周", "WEEKLY"),
    AgentFieldOption(null, "每月", "MONTHLY"),
    AgentFieldOption(null, "每年", "YEARLY"),
)

private val enabledOptions = listOf(
    AgentFieldOption(null, "启用", "true"),
    AgentFieldOption(null, "停用", "false"),
)
