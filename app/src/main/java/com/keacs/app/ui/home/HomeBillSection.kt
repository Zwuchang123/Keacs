package com.keacs.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.local.entity.RecordEntity
import com.keacs.app.domain.model.RecordType
import com.keacs.app.ui.components.EmptyState
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.RecordListItem
import com.keacs.app.ui.management.colorFor
import com.keacs.app.ui.management.iconFor
import com.keacs.app.ui.theme.KeacsColors

@Composable
fun MonthlyBillSection(
    groups: List<HomeDailyBillGroup>,
    categories: Map<Long, CategoryEntity>,
    accounts: Map<Long, AccountEntity>,
    onRecordClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    KeacsCard(modifier = modifier.fillMaxWidth()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (groups.isEmpty()) {
                EmptyState(
                    title = "暂无记录",
                    description = "这个月还没有账单",
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    itemsIndexed(groups, key = { _, group -> group.dayStart }) { groupIndex, group ->
                        DailyBillGroup(
                            group = group,
                            categories = categories,
                            accounts = accounts,
                            onRecordClick = onRecordClick,
                        )
                        if (groupIndex != groups.lastIndex) {
                            HorizontalDivider(
                                color = KeacsColors.Border,
                                thickness = 0.8.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyBillGroup(
    group: HomeDailyBillGroup,
    categories: Map<Long, CategoryEntity>,
    accounts: Map<Long, AccountEntity>,
    onRecordClick: (Long) -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = group.monthDay,
                    color = KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = group.weekDay,
                    color = KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = "支出 ${HomeViewModel.formatCent(group.expense)}",
                color = KeacsColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }

        group.records.forEachIndexed { index, record ->
            RecordListItem(
                icon = if (record.type == RecordType.TRANSFER) {
                    Icons.Rounded.AccountBalanceWallet
                } else {
                    iconFor(categories[record.categoryId]?.iconKey ?: "more")
                },
                iconColor = if (record.type == RecordType.TRANSFER) {
                    KeacsColors.Primary
                } else {
                    colorFor(categories[record.categoryId]?.colorKey ?: "gray")
                },
                title = recordTitle(record, categories, accounts),
                amount = recordAmount(record),
                amountColor = recordColor(record),
                compact = true,
                modifier = Modifier.clickable { onRecordClick(record.id) },
            )
            if (index != group.records.lastIndex) {
                HorizontalDivider(
                    color = KeacsColors.Border.copy(alpha = 0.42f),
                    thickness = 0.5.dp,
                )
            }
        }
    }
}

private fun recordTitle(
    record: RecordEntity,
    categories: Map<Long, CategoryEntity>,
    accounts: Map<Long, AccountEntity>,
): String {
    val category = categories[record.categoryId]
    return when (record.type) {
        RecordType.INCOME -> category?.name ?: "收入"
        RecordType.TRANSFER -> "${accounts[record.fromAccountId]?.name ?: "转出账户"} → ${accounts[record.toAccountId]?.name ?: "转入账户"}"
        else -> category?.name ?: "支出"
    }
}

private fun recordAmount(record: RecordEntity): String =
    when (record.type) {
        RecordType.INCOME -> "+${HomeViewModel.formatCent(record.amountCent)}"
        RecordType.EXPENSE -> "-${HomeViewModel.formatCent(record.amountCent)}"
        else -> HomeViewModel.formatCent(record.amountCent)
    }

private fun recordColor(record: RecordEntity): Color = KeacsColors.TextPrimary
