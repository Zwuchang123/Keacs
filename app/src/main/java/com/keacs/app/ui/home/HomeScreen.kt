package com.keacs.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.components.EmptyState
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.theme.AccountIcon
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
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.Section),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        OverviewCard()
        QuickActionRow(
            onAddClick = onAddClick,
            onRecordsClick = onRecordsClick,
            onStatsClick = onStatsClick,
            onMineClick = onMineClick,
        )
        RecentRecords(onAddClick = onAddClick)
    }
}

@Composable
private fun OverviewCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = KeacsColors.Primary,
                shape = MaterialTheme.shapes.large,
            )
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
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AmountSummary(label = "收入", amount = "¥0.00", color = KeacsColors.PrimaryLight)
                AmountSummary(label = "支出", amount = "¥0.00", color = KeacsColors.PrimaryLight)
                AmountSummary(label = "净资产", amount = "¥0.00", color = KeacsColors.PrimaryLight)
            }
        }
    }
}

@Composable
private fun AmountSummary(
    label: String,
    amount: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Column {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = amount,
            color = KeacsColors.Surface,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun QuickActionRow(
    onAddClick: () -> Unit,
    onRecordsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onMineClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickAction(
            text = "记一笔",
            icon = Icons.Rounded.Add,
            onClick = onAddClick,
            modifier = Modifier.weight(1f),
        )
        QuickAction(
            text = "账单",
            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
            onClick = onRecordsClick,
            modifier = Modifier.weight(1f),
        )
        QuickAction(
            text = "图表",
            icon = Icons.Rounded.BarChart,
            onClick = onStatsClick,
            modifier = Modifier.weight(1f),
        )
        QuickAction(
            text = "账户",
            icon = AccountIcon,
            onClick = onMineClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(KeacsColors.PrimaryLight, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = KeacsColors.Primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun RecentRecords(onAddClick: () -> Unit) {
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
                Spacer(modifier = Modifier.width(8.dp))
            }
            Spacer(modifier = Modifier.height(18.dp))
            EmptyState(
                title = "还没有账单",
                actionText = "记一笔",
                onActionClick = onAddClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp),
            )
        }
    }
}
