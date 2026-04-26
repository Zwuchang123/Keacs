package com.keacs.app.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.keacs.app.ui.components.WheelPickerBottomSheet
import com.keacs.app.ui.management.colorFor
import com.keacs.app.ui.management.iconFor
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    repository: LocalDataRepository,
    onViewRecord: (Long) -> Unit,
    onEditRecord: (Long) -> Unit,
) {
    val allRecords by repository.observeRecords().collectAsState(initial = emptyList())
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val accounts by repository.observeAccounts().collectAsState(initial = emptyList())

    var selectedYearMonth by remember { mutableStateOf(getCurrentYearMonth()) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val categoryMap = categories.associateBy { it.id }
    val accountMap = accounts.associateBy { it.id }

    val monthRecords = allRecords.filter { record ->
        isInYearMonth(record.occurredAt, selectedYearMonth)
    }.sortedByDescending { it.occurredAt }

    val groupedRecords = monthRecords.groupBy { formatRecordDateLabel(it.occurredAt) }

    val monthIncome = totalIncome(monthRecords)
    val monthExpense = totalExpense(monthRecords)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-records"),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = KeacsSpacing.PageHorizontal,
                vertical = KeacsSpacing.PageVertical,
            ),
        ) {
            SearchBox(text = "搜索账单")
            Spacer(modifier = Modifier.height(KeacsSpacing.Section))
            MonthSummaryCard(
                yearMonth = selectedYearMonth,
                income = monthIncome,
                expense = monthExpense,
                onMonthClick = { showMonthPicker = true },
            )
        }

        if (monthRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = KeacsSpacing.PageHorizontal),
            ) {
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = KeacsSpacing.PageHorizontal),
                verticalArrangement = Arrangement.spacedBy(KeacsSpacing.ItemGap),
            ) {
                groupedRecords.forEach { (date, records) ->
                    item(key = "header_$date") {
                        DateGroupHeader(
                            date = date,
                            dayNetAmount = calculateDayNetAmount(records),
                        )
                    }
                    item(key = "card_$date") {
                        KeacsCard {
                            Column(modifier = Modifier.padding(it)) {
                                records.forEach { record ->
                                    RecordListItem(
                                        icon = if (record.type == RecordType.TRANSFER) Icons.Rounded.AccountBalanceWallet
                                               else iconFor(categoryMap[record.categoryId]?.iconKey ?: "more"),
                                        iconColor = if (record.type == RecordType.TRANSFER) KeacsColors.Primary
                                                    else colorFor(categoryMap[record.categoryId]?.colorKey ?: "gray"),
                                        title = recordTitle(record, categoryMap, accountMap),
                                        note = record.note ?: "无备注",
                                        account = recordAccount(record, accountMap),
                                        amount = recordAmount(record),
                                        amountColor = recordColor(record),
                                        modifier = Modifier.clickable { onViewRecord(record.id) },
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(KeacsSpacing.Section))
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerBottomSheet(
            currentYearMonth = selectedYearMonth,
            allRecords = allRecords,
            onMonthSelected = { yearMonth ->
                selectedYearMonth = yearMonth
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false },
        )
    }
}

@Composable
private fun MonthSummaryCard(
    yearMonth: String,
    income: Long,
    expense: Long,
    onMonthClick: () -> Unit,
) {
    KeacsCard {
        Column(modifier = Modifier.padding(it)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onMonthClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = yearMonth,
                    color = KeacsColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "选择月份",
                    tint = KeacsColors.TextSecondary,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SummaryAmount("收入", formatCent(income), KeacsColors.Income)
                SummaryAmount("支出", formatCent(expense), KeacsColors.Expense)
            }
        }
    }
}

@Composable
private fun SummaryAmount(label: String, amount: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = " $amount",
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun DateGroupHeader(date: String, dayNetAmount: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = date,
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = formatCent(dayNetAmount),
            color = when {
                dayNetAmount > 0 -> KeacsColors.Income
                dayNetAmount < 0 -> KeacsColors.Expense
                else -> KeacsColors.TextTertiary
            },
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthPickerBottomSheet(
    currentYearMonth: String,
    allRecords: List<RecordEntity>,
    onMonthSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val availableMonths = remember(allRecords) {
        allRecords.asSequence()
            .map { formatYearMonth(it.occurredAt) }
            .distinct()
            .sortedDescending()
            .toList()
            .ifEmpty { listOf(getCurrentYearMonth()) }
    }

    val currentIndex = availableMonths.indexOf(currentYearMonth).coerceAtLeast(0)

    WheelPickerBottomSheet(
        title = "选择月份",
        items = availableMonths,
        selectedIndex = currentIndex,
        itemToString = { it },
        onItemSelected = { index ->
            onMonthSelected(availableMonths[index])
        },
        onDismiss = onDismiss,
    )
}

private fun recordTitle(
    record: RecordEntity,
    categoryMap: Map<Long, CategoryEntity>,
    accountMap: Map<Long, AccountEntity>,
): String = when (record.type) {
    RecordType.INCOME -> categoryMap[record.categoryId]?.name ?: "收入"
    RecordType.TRANSFER -> "${accountMap[record.fromAccountId]?.name ?: "账户"} → ${accountMap[record.toAccountId]?.name ?: "账户"}"
    else -> categoryMap[record.categoryId]?.name ?: "支出"
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

private fun calculateDayNetAmount(records: List<RecordEntity>): Long =
    records.sumOf { record ->
        when (record.type) {
            RecordType.INCOME -> record.amountCent
            RecordType.EXPENSE -> -record.amountCent
            else -> 0L
        }
    }

private fun formatCent(value: Long): String =
    "¥" + DecimalFormat("#,##0.00").format(value / 100.0)

private val dateLabelFormat = SimpleDateFormat("MM月dd日 E", Locale.getDefault())
private val yearMonthFormat = SimpleDateFormat("yyyy年MM月", Locale.getDefault())

private fun formatRecordDateLabel(timestamp: Long): String = dateLabelFormat.format(Date(timestamp))
private fun formatYearMonth(timestamp: Long): String = yearMonthFormat.format(Date(timestamp))

private fun getCurrentYearMonth(): String {
    val calendar = Calendar.getInstance()
    return yearMonthFormat.format(calendar.time)
}

private fun isInYearMonth(timestamp: Long, yearMonth: String): Boolean {
    val recordCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val targetCalendar = Calendar.getInstance().apply { time = yearMonthFormat.parse(yearMonth) ?: return false }
    return recordCalendar.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR) &&
           recordCalendar.get(Calendar.MONTH) == targetCalendar.get(Calendar.MONTH)
}
