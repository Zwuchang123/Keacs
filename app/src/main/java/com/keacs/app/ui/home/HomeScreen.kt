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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.EmptyState
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onRecordsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onMineClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-home")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        OverviewCard(onClick = onStatsClick)
        QuickActionCard(
            onAddClick = onAddClick,
            onRecordsClick = onRecordsClick,
            onStatsClick = onStatsClick,
            onMineClick = onMineClick,
        )
        RecentRecords()
    }
}

@Composable
private fun OverviewCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipLarge()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF65A2FF), KeacsColors.Primary),
                ),
            )
            .clickable(onClick = onClick)
            .padding(KeacsSpacing.CardPadding),
    ) {
        Column {
            Text(
                text = "本月结余",
                color = KeacsColors.PrimaryLight,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "¥0.00",
                color = KeacsColors.Surface,
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AmountSummary(
                    label = "收入",
                    amount = "¥0.00",
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
                    amount = "¥0.00",
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
    onRecordsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onMineClick: () -> Unit,
) {
    KeacsCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            QuickAction("记账", Icons.Rounded.Edit, KeacsColors.Primary, onAddClick)
            QuickAction("账户", Icons.Rounded.AccountBalanceWallet, KeacsColors.Income, onMineClick)
            QuickAction("图表", Icons.Rounded.BarChart, KeacsColors.Warning, onStatsClick)
            QuickAction("更多", Icons.Rounded.MoreHoriz, KeacsColors.TextSecondary, onRecordsClick)
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
private fun RecentRecords() {
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
                )
            }
            EmptyState(
                title = "暂无记录",
                description = "快去记一笔吧，养成记账习惯",
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(226.dp),
            )
        }
    }
}

@Composable
private fun Modifier.clipLarge(): Modifier = clip(MaterialTheme.shapes.large)
