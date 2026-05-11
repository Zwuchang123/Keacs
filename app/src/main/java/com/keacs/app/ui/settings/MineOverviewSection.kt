package com.keacs.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.rule.balanceFor
import com.keacs.app.domain.rule.totalExpense
import com.keacs.app.domain.rule.totalIncome
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.formatCent
import com.keacs.app.ui.components.formatGroupedCent
import com.keacs.app.ui.record.DatePickerMode
import com.keacs.app.ui.record.DateWheelPickerBottomSheet
import com.keacs.app.ui.theme.KeacsColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MineOverviewSection(repository: LocalDataRepository) {
    val records by repository.observeRecords().collectAsState(initial = emptyList())
    val accounts by repository.observeAccounts().collectAsState(initial = emptyList())
    var selectedMonth by remember { mutableStateOf(currentMonthStart()) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val (monthStart, monthEnd) = monthRange(selectedMonth)
    val monthRecords = records.filter { it.occurredAt in monthStart until monthEnd }

    val income = totalIncome(monthRecords)
    val expense = totalExpense(monthRecords)
    val asset = accounts
        .filter { it.nature == PresetSeedData.ACCOUNT_ASSET && it.isEnabled }
        .sumOf { balanceFor(it, records) }
    val liability = accounts
        .filter { it.nature == PresetSeedData.ACCOUNT_LIABILITY && it.isEnabled }
        .sumOf { balanceFor(it, records) }

    MonthOverviewCard(
        selectedMonth = selectedMonth,
        income = income,
        expense = expense,
        balance = income - expense,
        onMonthClick = { showMonthPicker = true },
    )
    AssetDebtCard(
        asset = asset,
        liability = liability,
        netAsset = asset + liability,
    )

    if (showMonthPicker) {
        DateWheelPickerBottomSheet(
            title = "选择月份",
            selectedDate = selectedMonth,
            mode = DatePickerMode.MONTH,
            onSelected = {
                selectedMonth = startOfMonth(it)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false },
        )
    }
}

@Composable
private fun MonthOverviewCard(
    selectedMonth: Long,
    income: Long,
    expense: Long,
    balance: Long,
    onMonthClick: () -> Unit,
) {
    KeacsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "当月概览",
                    color = KeacsColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(KeacsColors.SurfaceSubtle)
                        .clickable(onClick = onMonthClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatMonthText(selectedMonth),
                        color = KeacsColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "选择月份",
                        tint = KeacsColors.TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            MetricGrid(
                items = listOf(
                    MetricItem("收入", income, KeacsColors.Primary, Icons.Rounded.NorthEast),
                    MetricItem("支出", expense, KeacsColors.Expense, Icons.Rounded.SouthWest),
                    MetricItem("结余", balance, KeacsColors.Primary, Icons.Rounded.AccountBalanceWallet),
                ),
            )
        }
    }
}

@Composable
private fun AssetDebtCard(
    asset: Long,
    liability: Long,
    netAsset: Long,
) {
    KeacsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "资产负债",
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            NetAssetBlock(netAsset = netAsset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CompactMetric("资产", asset, Modifier.weight(1f))
                CompactMetric("负债", liability, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NetAssetBlock(netAsset: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KeacsColors.PrimaryLight)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "净资产",
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = formatGroupedCent(netAsset),
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetricGrid(items: List<MetricItem>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(KeacsColors.SurfaceSubtle)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(item.color.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = item.color,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Text(
                        text = item.label,
                        color = KeacsColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = formatCent(item.amount),
                        color = KeacsColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactMetric(
    label: String,
    amount: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(KeacsColors.SurfaceSubtle)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = formatGroupedCent(amount),
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class MetricItem(
    val label: String,
    val amount: Long,
    val color: Color,
    val icon: ImageVector,
)

private fun currentMonthStart(): Long = startOfMonth(System.currentTimeMillis())

private fun monthRange(month: Long): Pair<Long, Long> {
    val calendar = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = startOfMonth(month) }
    val start = calendar.timeInMillis
    calendar.add(Calendar.MONTH, 1)
    return start to calendar.timeInMillis
}

private fun startOfMonth(timestamp: Long): Long {
    val calendar = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = timestamp }
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun formatMonthText(timestamp: Long): String =
    SimpleDateFormat("yyyy年MM月", Locale.getDefault()).format(Date(timestamp))
