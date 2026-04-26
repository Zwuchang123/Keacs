package com.keacs.app.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.keacs.app.ui.components.YearMonthWheelPicker
import com.keacs.app.ui.management.OptionChip
import com.keacs.app.ui.management.colorFor
import com.keacs.app.ui.management.iconFor
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CategoryGrid(categories: List<CategoryEntity>, selectedId: Long?, onSelected: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(5).forEach { row ->
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
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (selected) KeacsColors.Primary else colorFor(category.colorKey),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconFor(category.iconKey),
                contentDescription = null,
                tint = if (selected) KeacsColors.Surface else KeacsColors.Surface,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = category.name,
            color = if (selected) KeacsColors.Primary else KeacsColors.TextPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferAccounts(
    accounts: List<AccountEntity>,
    fromId: Long?,
    toId: Long?,
    onFrom: (Long) -> Unit,
    onTo: (Long) -> Unit,
) {
    var showFromSelector by remember { mutableStateOf(false) }
    var showToSelector by remember { mutableStateOf(false) }

    val fromAccount = accounts.find { it.id == fromId }
    val toAccount = accounts.find { it.id == toId }

    KeacsCard(contentPadding = PaddingValues(12.dp)) {
        Column(Modifier.padding(it), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showFromSelector = true }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(KeacsColors.Expense.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = KeacsColors.Expense,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "转出账户",
                        color = KeacsColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = fromAccount?.name ?: "选择账户",
                        color = if (fromAccount != null) KeacsColors.TextPrimary else KeacsColors.TextTertiary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = KeacsColors.TextTertiary,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showToSelector = true }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(KeacsColors.Income.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = KeacsColors.Income,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "转入账户",
                        color = KeacsColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = toAccount?.name ?: "选择账户",
                        color = if (toAccount != null) KeacsColors.TextPrimary else KeacsColors.TextTertiary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = KeacsColors.TextTertiary,
                )
            }
        }
    }

    if (showFromSelector) {
        AccountSelectorBottomSheet(
            accounts = accounts,
            selectedId = fromId,
            title = "选择转出账户",
            onSelected = { onFrom(it) },
            onDismiss = { showFromSelector = false },
        )
    }

    if (showToSelector) {
        AccountSelectorBottomSheet(
            accounts = accounts,
            selectedId = toId,
            title = "选择转入账户",
            onSelected = { onTo(it) },
            onDismiss = { showToSelector = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSelectorBottomSheet(
    accounts: List<AccountEntity>,
    selectedId: Long?,
    title: String,
    onSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedIndex = accounts.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KeacsColors.Surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KeacsSpacing.PageHorizontal)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = title,
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(accounts) { index, account ->
                    val isSelected = account.id == selectedId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (isSelected) KeacsColors.PrimaryLight else KeacsColors.SurfaceSubtle,
                            )
                            .clickable {
                                onSelected(account.id)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colorFor(account.colorKey).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = iconFor(account.iconKey),
                                contentDescription = null,
                                tint = colorFor(account.colorKey),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = account.name,
                            color = if (isSelected) KeacsColors.Primary else KeacsColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "已选",
                                tint = KeacsColors.Primary,
                            )
                        }
                    }
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormArea(
    accounts: List<AccountEntity>,
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

    val selectedAccount = accounts.find { it.id == accountId }
    val calendar = remember(occurredAt) {
        java.util.Calendar.getInstance().apply { timeInMillis = occurredAt }
    }
    val currentYear = calendar.get(java.util.Calendar.YEAR)
    val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
    val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)

    KeacsCard(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
        Column(Modifier.padding(it), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (showAccount) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAccountSelector = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = KeacsColors.TextSecondary,
                        modifier = Modifier.size(21.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "账户",
                            color = KeacsColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = selectedAccount?.name ?: "不选账户",
                            color = if (selectedAccount != null) KeacsColors.TextPrimary else KeacsColors.TextTertiary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = KeacsColors.TextTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDateSelector = true }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = KeacsColors.TextSecondary,
                    modifier = Modifier.size(21.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "日期",
                        color = KeacsColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = dateLabel(occurredAt),
                        color = KeacsColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = KeacsColors.TextTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }

            NoteField(note, onNoteChange)
        }
    }

    if (showAccountSelector) {
        AccountSelectorBottomSheet(
            accounts = accounts,
            selectedId = accountId,
            title = "选择账户",
            onSelected = { onAccountSelected(it) },
            onDismiss = { showAccountSelector = false },
        )
    }

    if (showDateSelector) {
        YearMonthWheelPicker(
            currentYear = currentYear,
            currentMonth = currentMonth,
            onDateSelected = { year, month ->
                val cal = java.util.Calendar.getInstance(Locale.getDefault())
                cal.set(year, month - 1, currentDay, 0, 0, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                onDateSelected(cal.timeInMillis)
            },
            onDismiss = { showDateSelector = false },
        )
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
