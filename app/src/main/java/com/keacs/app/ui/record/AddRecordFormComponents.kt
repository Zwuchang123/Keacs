package com.keacs.app.ui.record

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.ui.components.AmountText
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.FormFieldRow
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.NumberPad
import com.keacs.app.ui.management.OptionChip
import com.keacs.app.ui.management.colorFor
import com.keacs.app.ui.management.iconFor
import com.keacs.app.ui.theme.KeacsColors

@Composable
fun CategoryGrid(categories: List<CategoryEntity>, selectedId: Long?, onSelected: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.take(10).chunked(5).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { category ->
                    CategoryChoice(category, selectedId == category.id, Modifier.weight(1f)) { onSelected(category.id) }
                }
                repeat(5 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CategoryChoice(category: CategoryEntity, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CategoryIcon(
            icon = iconFor(category.iconKey),
            backgroundColor = if (selected) KeacsColors.Primary else colorFor(category.colorKey),
        )
        Text(
            text = category.name,
            color = if (selected) KeacsColors.Primary else KeacsColors.TextPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
fun TransferAccounts(
    accounts: List<AccountEntity>,
    fromId: Long?,
    toId: Long?,
    onFrom: (Long) -> Unit,
    onTo: (Long) -> Unit,
) {
    KeacsCard(contentPadding = PaddingValues(12.dp)) {
        Column(Modifier.padding(it), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AccountChips("转出", accounts, fromId, onFrom)
            AccountChips("转入", accounts, toId, onTo)
        }
    }
}

@Composable
private fun AccountChips(title: String, accounts: List<AccountEntity>, selectedId: Long?, onSelected: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = KeacsColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        accounts.take(6).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { account ->
                    OptionChip(account.name, selectedId == account.id, Modifier.weight(1f)) { onSelected(account.id) }
                }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
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
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AmountText(amount = if (amount.isBlank()) "¥0.00" else "¥$amount")
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
    accountId: Long?,
    showAccount: Boolean,
    occurredAt: Long,
    note: String,
    onAccountSelected: (Long?) -> Unit,
    onToday: () -> Unit,
    onYesterday: () -> Unit,
    onNoteChange: (String) -> Unit,
) {
    KeacsCard(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
        Column(Modifier.padding(it), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (showAccount) {
                FormFieldRow(Icons.Rounded.AccountBalanceWallet, "账户", accounts.firstOrNull { it.id == accountId }?.name ?: "不选账户")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OptionChip("不选", accountId == null, Modifier.weight(1f)) { onAccountSelected(null) }
                    accounts.take(2).forEach { account ->
                        OptionChip(account.name, accountId == account.id, Modifier.weight(1f)) { onAccountSelected(account.id) }
                    }
                }
            }
            FormFieldRow(Icons.Rounded.CalendarToday, "日期", dateLabel(occurredAt))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OptionChip("今天", isSameDay(occurredAt, System.currentTimeMillis()), Modifier.weight(1f), onToday)
                OptionChip("昨天", isSameDay(occurredAt, System.currentTimeMillis() - ONE_DAY), Modifier.weight(1f), onYesterday)
            }
            NoteField(note, onNoteChange)
        }
    }
}

@Composable
private fun NoteField(note: String, onNoteChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
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
