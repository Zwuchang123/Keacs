package com.keacs.app.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.ui.components.AmountText
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.FormFieldRow
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.NumberPad
import com.keacs.app.ui.management.accountIconOptionFor
import com.keacs.app.ui.management.colorFor
import com.keacs.app.ui.theme.KeacsColors

@Composable
fun TransferAccounts(
    accounts: List<AccountEntity>,
    accountCategories: List<CategoryEntity>,
    fromId: Long?,
    toId: Long?,
    onFrom: (Long) -> Unit,
    onTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFromSelector by remember { mutableStateOf(false) }
    var showToSelector by remember { mutableStateOf(false) }
    val fromAccount = accounts.firstOrNull { it.id == fromId }
    val toAccount = accounts.firstOrNull { it.id == toId }

    KeacsCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(it),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TransferAccountBox(
                title = "转出账户",
                account = fromAccount,
                accountCategories = accountCategories,
                modifier = Modifier.weight(1f),
                onClick = { showFromSelector = true },
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(KeacsColors.PrimaryLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "转入方向",
                    tint = KeacsColors.Primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            TransferAccountBox(
                title = "转入账户",
                account = toAccount,
                accountCategories = accountCategories,
                modifier = Modifier.weight(1f),
                onClick = { showToSelector = true },
            )
        }
    }

    if (showFromSelector) {
        AccountSelectorBottomSheet(
            accounts = accounts,
            accountCategories = accountCategories,
            selectedId = fromId,
            title = "选择转出账户",
            includeNone = false,
            onSelected = { it?.let(onFrom) },
            onDismiss = { showFromSelector = false },
        )
    }
    if (showToSelector) {
        AccountSelectorBottomSheet(
            accounts = accounts,
            accountCategories = accountCategories,
            selectedId = toId,
            title = "选择转入账户",
            includeNone = false,
            onSelected = { it?.let(onTo) },
            onDismiss = { showToSelector = false },
        )
    }
}

@Composable
fun AmountKeyboardPanel(
    amount: String,
    parsedAmount: Long?,
    message: String?,
    saveEnabled: Boolean,
    onKeyClick: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeacsCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            AmountText(amount = amount.ifBlank { "0.00" })
            Text(
                text = message ?: if (parsedAmount == null) "金额大于0才可保存" else " ",
                color = if (message == null) KeacsColors.TextTertiary else KeacsColors.Error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            NumberPad(
                saveEnabled = saveEnabled,
                onKeyClick = onKeyClick,
                onSaveClick = onSaveClick,
            )
        }
    }
}

@Composable
fun RecordErrorText(message: String?) {
    if (message != null) {
        Text(
            text = message,
            color = KeacsColors.Error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun FormArea(
    accounts: List<AccountEntity>,
    accountCategories: List<CategoryEntity>,
    accountId: Long?,
    showAccount: Boolean,
    occurredAt: Long,
    note: String,
    onAccountSelected: (Long?) -> Unit,
    onDateSelected: (Long) -> Unit,
    onNoteChange: (String) -> Unit,
) {
    var showAccountSelector by remember { mutableStateOf(false) }
    var showDateSelector by remember { mutableStateOf(false) }
    val selectedAccount = accounts.firstOrNull { it.id == accountId }
    val selectedAccountIcon = accountIconOptionFor(selectedAccount, accountCategories)

    KeacsCard(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
        Column(Modifier.padding(it), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showAccount) {
                FormFieldRow(
                    selectedAccountIcon.icon,
                    "账户",
                    selectedAccount?.name ?: "未选择",
                    modifier = Modifier.clickable { showAccountSelector = true },
                )
            }
            FormFieldRow(
                Icons.Rounded.CalendarToday,
                "日期",
                dateLabel(occurredAt),
                modifier = Modifier.clickable { showDateSelector = true },
            )
            NoteField(note, onNoteChange)
        }
    }

    if (showAccountSelector) {
        AccountSelectorBottomSheet(
            accounts = accounts,
            accountCategories = accountCategories,
            selectedId = accountId,
            title = "选择账户",
            includeNone = false,
            onSelected = onAccountSelected,
            onDismiss = { showAccountSelector = false },
        )
    }
    if (showDateSelector) {
        DateWheelPickerBottomSheet(
            title = "选择日期",
            selectedDate = occurredAt,
            mode = DatePickerMode.DAY,
            onSelected = {
                onDateSelected(it)
                showDateSelector = false
            },
            onDismiss = { showDateSelector = false },
        )
    }
}

@Composable
private fun TransferAccountBox(
    title: String,
    account: AccountEntity?,
    accountCategories: List<CategoryEntity>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val iconOption = accountIconOptionFor(account, accountCategories)
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(KeacsColors.SurfaceSubtle)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CategoryIcon(
            icon = iconOption.icon,
            backgroundColor = colorFor(iconOption.colorKey),
        )
        Text(title, color = KeacsColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        Text(
            text = account?.name ?: "选择账户",
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NoteField(note: String, onNoteChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 12.dp),
    ) {
        Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null, tint = KeacsColors.TextSecondary)
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = note,
            onValueChange = onNoteChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = KeacsColors.TextPrimary),
            cursorBrush = SolidColor(KeacsColors.Primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (note.isBlank()) Text("添加备注", color = KeacsColors.TextTertiary, style = MaterialTheme.typography.bodyMedium)
                inner()
            },
        )
    }
}

@Composable
fun DeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除这笔账？") },
        text = { Text("删除后，账户余额和收支统计会一起修正。") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("删除", color = KeacsColors.Error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
