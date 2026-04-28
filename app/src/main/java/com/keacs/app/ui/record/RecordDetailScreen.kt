package com.keacs.app.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.local.entity.RecordEntity
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.model.RecordType
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.management.colorFor
import com.keacs.app.ui.management.iconFor
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordDetailScreen(
    recordId: Long,
    repository: LocalDataRepository,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val records by repository.observeRecords().collectAsState(initial = emptyList())
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val accounts by repository.observeAccounts().collectAsState(initial = emptyList())

    val record = records.find { it.id == recordId }
    val category = record?.categoryId?.let { catId -> categories.find { it.id == catId } }
    val fromAccount = record?.fromAccountId?.let { accId -> accounts.find { it.id == accId } }
    val toAccount = record?.toAccountId?.let { accId -> accounts.find { it.id == accId } }

    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-record-detail")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        if (record == null) {
            KeacsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(it),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "记录不存在",
                        color = KeacsColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            RecordDetailCard(
                record = record,
                category = category,
                fromAccount = fromAccount,
                toAccount = toAccount,
            )
            RecordInfoCard(
                record = record,
                category = category,
                fromAccount = fromAccount,
                toAccount = toAccount,
            )

            Button(
                onClick = { onEdit(recordId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KeacsColors.Primary,
                    contentColor = KeacsColors.Surface,
                ),
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("编辑记录")
            }

            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = KeacsColors.Error,
                ),
            ) {
                Text("删除记录")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除记录") },
            text = { Text("确定要删除这条记录吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.deleteRecord(recordId)
                            showDeleteDialog = false
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = KeacsColors.Error),
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun RecordDetailCard(
    record: RecordEntity,
    category: CategoryEntity?,
    fromAccount: AccountEntity?,
    toAccount: AccountEntity?,
) {
    val iconColor = when (record.type) {
        RecordType.TRANSFER -> KeacsColors.Primary
        else -> colorFor(category?.colorKey ?: "gray")
    }
    val icon = when (record.type) {
        RecordType.TRANSFER -> Icons.Rounded.AccountBalanceWallet
        else -> iconFor(category?.iconKey ?: "more")
    }
    val title = when (record.type) {
        RecordType.INCOME -> category?.name ?: "收入"
        RecordType.TRANSFER -> "转账"
        else -> category?.name ?: "支出"
    }
    val amountColor = when (record.type) {
        RecordType.INCOME -> KeacsColors.Income
        RecordType.EXPENSE -> KeacsColors.Expense
        else -> KeacsColors.TextPrimary
    }
    val amountPrefix = when (record.type) {
        RecordType.INCOME -> "+"
        RecordType.EXPENSE -> "-"
        else -> ""
    }

    KeacsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CategoryIcon(
                icon = icon,
                backgroundColor = iconColor,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$amountPrefix${formatCent(record.amountCent)}",
                color = amountColor,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RecordInfoCard(
    record: RecordEntity,
    category: CategoryEntity?,
    fromAccount: AccountEntity?,
    toAccount: AccountEntity?,
) {
    KeacsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it),
        ) {
            when (record.type) {
                RecordType.TRANSFER -> {
                    InfoRow(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        label = "转出账户",
                        value = fromAccount?.name ?: "未选择",
                        valueColor = KeacsColors.Expense,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 0.5.dp,
                        color = KeacsColors.Border,
                    )
                    InfoRow(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        label = "转入账户",
                        value = toAccount?.name ?: "未选择",
                        valueColor = KeacsColors.Income,
                    )
                }
                else -> {
                    val accountLabel = if (record.type == RecordType.INCOME) "收入账户" else "支出账户"
                    val account = if (record.type == RecordType.INCOME) toAccount else fromAccount
                    InfoRow(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        label = accountLabel,
                        value = account?.name ?: "未选择",
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 0.5.dp,
                color = KeacsColors.Border,
            )

            InfoRow(
                icon = Icons.Rounded.CalendarToday,
                label = "账目日期",
                value = formatDate(record.occurredAt),
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 0.5.dp,
                color = KeacsColors.Border,
            )
            InfoRow(
                icon = Icons.Rounded.Notes,
                label = "备注",
                value = record.note?.takeIf { it.isNotBlank() } ?: "未填写",
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = KeacsColors.TextPrimary,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = KeacsColors.TextSecondary,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
private val currencyFormat = DecimalFormat("#,##0.00")

private fun formatCent(value: Long): String =
    currencyFormat.format(value / 100.0)

private fun formatDate(timestamp: Long): String =
    dateFormat.format(Date(timestamp))
