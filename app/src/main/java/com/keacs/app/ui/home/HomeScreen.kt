package com.keacs.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.local.entity.RecordEntity
import com.keacs.app.domain.model.RecordType
import com.keacs.app.ui.components.CategoryIcon
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
    onAddClick: () -> Unit,
    onRecordClick: (Long) -> Unit,
    onRecordsClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-home")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        PrivacyBanner()
        OverviewCard(
            totalIncome = uiState.totalIncome,
            totalExpense = uiState.totalExpense,
            totalAsset = uiState.totalAsset,
            totalLiability = uiState.totalLiability,
        )
        QuickActionCard(
            onAddClick = onAddClick,
        )
        RecentRecords(
            records = uiState.recentRecords,
            categories = uiState.categories,
            onRecordClick = onRecordClick,
            onViewMoreClick = onRecordsClick,
        )
    }
}

@Composable
private fun PrivacyBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(KeacsColors.SurfaceSubtle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = KeacsColors.TextSecondary,
                modifier = Modifier.height(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "数据仅保存在本设备 · 支持支出导入和导出",
                color = KeacsColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun OverviewCard(
    totalIncome: Long,
    totalExpense: Long,
    totalAsset: Long,
    totalLiability: Long,
) {
    val netBalance = totalAsset - totalLiability

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
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "本月结余",
                    color = KeacsColors.PrimaryLight,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "净资产",
                    color = KeacsColors.PrimaryLight,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = HomeViewModel.formatCent(netBalance),
                    color = KeacsColors.Surface,
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = HomeViewModel.formatCent(totalAsset),
                    color = KeacsColors.Surface.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AmountSummary(
                    label = "收入",
                    amount = HomeViewModel.formatCent(totalIncome),
                    color = KeacsColors.Income,
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(KeacsColors.PrimaryLight.copy(alpha = 0.45f)),
                )
                AmountSummary(
                    label = "支出",
                    amount = HomeViewModel.formatCent(totalExpense),
                    color = KeacsColors.Expense,
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                )
            }
        }
    }
}

@Composable
private fun AmountSummary(
    label: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
    ) {
        Text(
            text = label,
            color = KeacsColors.PrimaryLight,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = amount,
            color = color,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun QuickActionCard(
    onAddClick: () -> Unit,
) {
    KeacsCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it),
            horizontalArrangement = Arrangement.Center,
        ) {
            QuickAction("记账", Icons.Rounded.Edit, KeacsColors.Primary, onAddClick)
        }
    }
}

@Composable
private fun QuickAction(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .height(76.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CategoryIcon(
            icon = icon,
            backgroundColor = color.copy(alpha = 0.14f),
            tint = color,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun RecentRecords(
    records: List<RecordEntity>,
    categories: Map<Long, CategoryEntity>,
    onRecordClick: (Long) -> Unit,
    onViewMoreClick: () -> Unit,
) {
    KeacsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    records.forEach { record ->
                        RecordListItem(
                            icon = if (record.type == RecordType.TRANSFER) Icons.Rounded.AccountBalanceWallet
                                   else iconFor(categories[record.categoryId]?.iconKey ?: "more"),
                            iconColor = if (record.type == RecordType.TRANSFER) KeacsColors.Primary
                                        else colorFor(categories[record.categoryId]?.colorKey ?: "gray"),
                            title = recordTitle(record, categories),
                            note = record.note ?: "无备注",
                            account = recordAccount(record),
                            amount = recordAmount(record),
                            amountColor = recordColor(record),
                            modifier = Modifier.clickable { onRecordClick(record.id) },
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

private fun recordAccount(record: RecordEntity): String {
    return when (record.type) {
        RecordType.INCOME -> "收入"
        RecordType.EXPENSE -> "支出"
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
