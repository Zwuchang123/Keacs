package com.keacs.app.ui.record

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.local.entity.RecordEntity
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.model.RecordType
import com.keacs.app.domain.rule.totalExpense
import com.keacs.app.domain.rule.totalIncome
import com.keacs.app.ui.components.EmptyState
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.RecordListItem
import com.keacs.app.ui.components.SearchBox
import com.keacs.app.ui.management.colorFor
import com.keacs.app.ui.management.iconFor
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import java.text.DecimalFormat

@Composable
fun RecordScreen(
    repository: LocalDataRepository,
    onEditRecord: (Long) -> Unit,
) {
    val records by repository.observeRecords().collectAsState(initial = emptyList())
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val accounts by repository.observeAccounts().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-records")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        SearchBox(text = "搜索账单")
        MonthSummaryCard(totalIncome(records), totalExpense(records))
        if (records.isEmpty()) {
            EmptyRecordCard()
        } else {
            RecordGroups(records, categories, accounts, onEditRecord)
        }
    }
}

@Composable
private fun MonthSummaryCard(income: Long, expense: Long) {
    KeacsCard {
        Column(modifier = Modifier.padding(it)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("本月", color = KeacsColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = KeacsColors.TextSecondary)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.CalendarToday, contentDescription = "选择月份", tint = KeacsColors.TextSecondary)
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryAmount("收入", formatCent(income), KeacsColors.Income)
                SummaryAmount("支出", formatCent(expense), KeacsColors.Expense)
            }
        }
    }
}

@Composable
private fun SummaryAmount(label: String, amount: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = KeacsColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
        Text(" $amount", color = color, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun RecordGroups(
    records: List<RecordEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onEditRecord: (Long) -> Unit,
) {
    val categoryMap = categories.associateBy { it.id }
    val accountMap = accounts.associateBy { it.id }
    records.groupBy { dateLabel(it.occurredAt) }.forEach { (date, items) ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(date, color = KeacsColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            KeacsCard {
                Column(modifier = Modifier.padding(it)) {
                    items.forEach { record ->
                        val category = record.categoryId?.let(categoryMap::get)
                        RecordListItem(
                            icon = if (record.type == RecordType.TRANSFER) Icons.Rounded.AccountBalanceWallet else iconFor(category?.iconKey ?: "more"),
                            iconColor = if (record.type == RecordType.TRANSFER) KeacsColors.Primary else colorFor(category?.colorKey ?: "gray"),
                            title = recordTitle(record, category, accountMap),
                            note = record.note ?: "无备注",
                            account = recordAccount(record, accountMap),
                            amount = recordAmount(record),
                            amountColor = recordColor(record),
                            modifier = Modifier.clickable { onEditRecord(record.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRecordCard() {
    KeacsCard {
        EmptyState(
            title = "暂无记录",
            description = "当前没有账单，快去记一笔吧",
            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .padding(it),
        )
    }
}

private fun recordTitle(
    record: RecordEntity,
    category: CategoryEntity?,
    accountMap: Map<Long, AccountEntity>,
): String = when (record.type) {
    RecordType.INCOME -> category?.name ?: "收入"
    RecordType.TRANSFER -> "${accountMap[record.fromAccountId]?.name ?: "账户"} 转入 ${accountMap[record.toAccountId]?.name ?: "账户"}"
    else -> category?.name ?: "支出"
}

private fun recordAccount(record: RecordEntity, accountMap: Map<Long, AccountEntity>): String =
    when (record.type) {
        RecordType.INCOME -> record.toAccountId?.let { accountMap[it]?.name } ?: "未选账户"
        RecordType.EXPENSE -> record.fromAccountId?.let { accountMap[it]?.name } ?: "未选账户"
        else -> "转账"
    }

private fun recordAmount(record: RecordEntity): String =
    when (record.type) {
        RecordType.INCOME -> "+${formatCent(record.amountCent)}"
        RecordType.EXPENSE -> "-${formatCent(record.amountCent)}"
        else -> formatCent(record.amountCent)
    }

private fun recordColor(record: RecordEntity): Color =
    when (record.type) {
        RecordType.INCOME -> KeacsColors.Income
        RecordType.EXPENSE -> KeacsColors.Expense
        else -> KeacsColors.TextPrimary
    }

private fun formatCent(value: Long): String =
    "¥" + DecimalFormat("#,##0.00").format(value / 100.0)
