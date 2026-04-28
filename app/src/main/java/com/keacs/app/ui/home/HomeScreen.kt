package com.keacs.app.ui.home

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.RecordEntity
import com.keacs.app.domain.model.RecordType
import com.keacs.app.ui.components.EmptyState
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.RecordListItem
import com.keacs.app.ui.management.colorFor
import com.keacs.app.ui.management.iconFor
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onRecordsClick: () -> Unit,
    onRecordClick: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-home")
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        OverviewCard(
            totalIncome = uiState.totalIncome,
            totalExpense = uiState.totalExpense,
        )
        RecentRecords(
            records = uiState.recentRecords,
            categories = uiState.categories,
            accounts = uiState.accounts,
            onRecordClick = onRecordClick,
            onViewMoreClick = onRecordsClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OverviewCard(
    totalIncome: Long,
    totalExpense: Long,
) {
    val monthBalance = totalIncome - totalExpense

    val animatedBalance by animateIntAsState(
        targetValue = monthBalance.toInt(),
        animationSpec = tween(durationMillis = 800),
        label = "animatedBalance"
    )
    val animatedIncome by animateIntAsState(
        targetValue = totalIncome.toInt(),
        animationSpec = tween(durationMillis = 800),
        label = "animatedIncome"
    )
    val animatedExpense by animateIntAsState(
        targetValue = totalExpense.toInt(),
        animationSpec = tween(durationMillis = 800),
        label = "animatedExpense"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipLarge()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF65A2FF), KeacsColors.Primary),
                ),
            )
            .padding(KeacsSpacing.CardPadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "本月结余",
                        color = KeacsColors.PrimaryLight,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = HomeViewModel.formatCent(animatedBalance.toLong()),
                        color = KeacsColors.Surface,
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OverviewPill(
                    label = "收入",
                    amount = HomeViewModel.formatCent(animatedIncome.toLong()),
                    icon = Icons.Rounded.NorthEast,
                    color = KeacsColors.Income,
                    modifier = Modifier.weight(1f),
                )
                OverviewPill(
                    label = "支出",
                    amount = HomeViewModel.formatCent(animatedExpense.toLong()),
                    icon = Icons.Rounded.SouthWest,
                    color = KeacsColors.Expense,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun OverviewPill(
    label: String,
    amount: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(22.dp)
                .clip(MaterialTheme.shapes.small)
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier
                    .width(13.dp)
                    .height(13.dp),
            )
        }
        Column {
            Text(
                text = label,
                color = KeacsColors.PrimaryLight,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = amount,
                color = KeacsColors.Surface,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun RecentRecords(
    records: List<RecordEntity>,
    categories: Map<Long, CategoryEntity>,
    accounts: Map<Long, AccountEntity>,
    onRecordClick: (Long) -> Unit,
    onViewMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeacsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .padding(it),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "最近记录",
                    color = KeacsColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "查看更多",
                    color = KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable(onClick = onViewMoreClick),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (records.isEmpty()) {
                EmptyState(
                    title = "暂无记录",
                    description = "快去记一笔吧，养成记账习惯",
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(records, key = { record -> record.id }) { record ->
                        RecordListItem(
                            icon = if (record.type == RecordType.TRANSFER) Icons.Rounded.AccountBalanceWallet
                                   else iconFor(categories[record.categoryId]?.iconKey ?: "more"),
                            iconColor = if (record.type == RecordType.TRANSFER) KeacsColors.Primary
                                        else colorFor(categories[record.categoryId]?.colorKey ?: "gray"),
                            title = recordTitle(record, categories),
                            note = record.note ?: "无备注",
                            account = recordAccount(record, accounts),
                            amount = recordAmount(record),
                            amountColor = recordColor(record),
                            modifier = Modifier
                                .clickable { onRecordClick(record.id) }
                        )
                    }
                }
            }
        }
    }
}

private fun recordTitle(record: RecordEntity, categories: Map<Long, CategoryEntity>): String {
    val category = categories[record.categoryId]
    return when (record.type) {
        RecordType.INCOME -> category?.name ?: "收入"
        RecordType.TRANSFER -> "转账"
        else -> category?.name ?: "支出"
    }
}

private fun recordAccount(record: RecordEntity, accounts: Map<Long, AccountEntity>): String {
    return when (record.type) {
        RecordType.INCOME -> record.toAccountId?.let { accounts[it]?.name } ?: "未选择"
        RecordType.EXPENSE -> record.fromAccountId?.let { accounts[it]?.name } ?: "未选择"
        else -> "转账"
    }
}

private fun recordAmount(record: RecordEntity): String =
    when (record.type) {
        RecordType.INCOME -> "+${HomeViewModel.formatCent(record.amountCent)}"
        RecordType.EXPENSE -> "-${HomeViewModel.formatCent(record.amountCent)}"
        else -> HomeViewModel.formatCent(record.amountCent)
    }

private fun recordColor(record: RecordEntity): Color =
    when (record.type) {
        RecordType.INCOME -> KeacsColors.Income
        RecordType.EXPENSE -> KeacsColors.Expense
        else -> KeacsColors.TextPrimary
    }

@Composable
private fun Modifier.clipLarge(): Modifier = clip(MaterialTheme.shapes.large)
