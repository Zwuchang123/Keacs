package com.keacs.app.ui.stats

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.SegmentedTabs
import com.keacs.app.ui.components.YearMonthWheelPicker
import com.keacs.app.ui.management.colorFor
import com.keacs.app.ui.management.iconFor
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(
    repository: LocalDataRepository,
    viewModel: StatsViewModel = viewModel { StatsViewModel(repository) },
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-stats")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        StatsTabSelector(
            selectedTab = uiState.selectedTab,
            onTabSelected = viewModel::selectTab,
        )

        PeriodSelector(
            selectedPeriod = uiState.selectedPeriod,
            selectedDate = uiState.selectedDate,
            onPeriodSelected = viewModel::selectPeriod,
            onDateSelected = viewModel::selectDate,
        )

        IncomeExpenseSummaryCard(
            totalAsset = uiState.totalAsset,
            totalLiability = uiState.totalLiability,
            netAsset = uiState.netAsset,
        )

        when (uiState.selectedTab) {
            StatsTab.EXPENSE, StatsTab.INCOME -> {
                TrendChartCard(
                    dailyTrend = uiState.dailyTrend,
                    period = uiState.selectedPeriod,
                )
                CategoryChartCard(
                    categoryStats = uiState.categoryStats,
                    tab = uiState.selectedTab,
                )
            }
            StatsTab.ASSET -> {
                AssetLiabilityCard(
                    totalAsset = uiState.totalAsset,
                    totalLiability = uiState.totalLiability,
                    netAsset = uiState.netAsset,
                )
                NetAssetTrendCard(
                    monthlyTrend = uiState.monthlyNetAssetTrend,
                )
                AccountBalanceListCard(
                    accountBalances = uiState.accountBalances,
                )
            }
        }
    }
}

@Composable
private fun StatsTabSelector(
    selectedTab: StatsTab,
    onTabSelected: (StatsTab) -> Unit,
) {
    SegmentedTabs(
        items = listOf("支出", "收入", "资产"),
        selectedIndex = selectedTab.ordinal,
        onSelected = { index -> onTabSelected(StatsTab.entries[index]) },
    )
}

@Composable
private fun PeriodSelector(
    selectedPeriod: TimePeriod,
    selectedDate: Long,
    onPeriodSelected: (TimePeriod) -> Unit,
    onDateSelected: (Long) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val calendar = remember(selectedDate) {
        Calendar.getInstance().apply { timeInMillis = selectedDate }
    }
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH) + 1

    val dateFormat = when (selectedPeriod) {
        TimePeriod.DAY -> SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
        TimePeriod.MONTH -> SimpleDateFormat("yyyy年MM月", Locale.getDefault())
        TimePeriod.YEAR -> SimpleDateFormat("yyyy年", Locale.getDefault())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KeacsColors.Surface)
            .clickable { showPicker = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dateFormat.format(Date(selectedDate)),
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "选择时间",
                tint = KeacsColors.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }

        PeriodTabs(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = onPeriodSelected,
        )
    }

    if (showPicker) {
        YearMonthWheelPicker(
            currentYear = currentYear,
            currentMonth = currentMonth,
            onDateSelected = { year, month ->
                val cal = Calendar.getInstance(Locale.getDefault())
                cal.set(year, month - 1, 1, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                if (selectedPeriod == TimePeriod.DAY) {
                    cal.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
                }
                onDateSelected(cal.timeInMillis)
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun PeriodTabs(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(KeacsColors.SurfaceSubtle)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        TimePeriod.entries.forEach { period ->
            val isSelected = period == selectedPeriod
            Text(
                text = when (period) {
                    TimePeriod.DAY -> "日"
                    TimePeriod.MONTH -> "月"
                    TimePeriod.YEAR -> "年"
                },
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(if (isSelected) KeacsColors.Primary else Color.Transparent)
                    .clickable { onPeriodSelected(period) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                color = if (isSelected) KeacsColors.Surface else KeacsColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun IncomeExpenseSummaryCard(
    totalAsset: Long,
    totalLiability: Long,
    netAsset: Long,
) {
    KeacsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatAmountItem(
                label = "总资产",
                amount = totalAsset,
                color = KeacsColors.Income,
            )
            StatAmountItem(
                label = "总负债",
                amount = totalLiability,
                color = KeacsColors.Expense,
            )
            StatAmountItem(
                label = "净资产",
                amount = netAsset,
                color = if (netAsset >= 0) KeacsColors.Primary else KeacsColors.Expense,
            )
        }
    }
}

@Composable
private fun StatAmountItem(
    label: String,
    amount: Long,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatCent(amount),
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TrendChartCard(
    dailyTrend: List<DailyStats>,
    period: TimePeriod,
) {
    KeacsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "收支趋势",
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (dailyTrend.isEmpty() || dailyTrend.all { it.amount == 0L }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无数据",
                        color = KeacsColors.TextTertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                TrendLineChart(
                    dailyTrend = dailyTrend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp),
                )
            }
        }
    }
}

@Composable
private fun TrendLineChart(
    dailyTrend: List<DailyStats>,
    modifier: Modifier = Modifier,
) {
    val gridColor = KeacsColors.Border.copy(alpha = 0.75f)
    val maxAmount = dailyTrend.maxOfOrNull { kotlin.math.abs(it.amount) } ?: 1L
    val minAmount = dailyTrend.minOfOrNull { it.amount } ?: 0L
    val range = (maxAmount - minAmount).coerceAtLeast(1L)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.width(32.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatShort(maxAmount),
                    color = KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = formatShort((maxAmount + minAmount) / 2),
                    color = KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = formatShort(minAmount),
                    color = KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(128.dp),
            ) {
                val chartHeight = size.height - 24.dp.toPx()
                val chartTop = 0f

                listOf(0f, 0.5f, 1f).forEach { ratio ->
                    val y = chartTop + chartHeight * ratio
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                }

                if (dailyTrend.isNotEmpty()) {
                    val points = dailyTrend.mapIndexed { index, stat ->
                        val x = size.width * index / (dailyTrend.size - 1).coerceAtLeast(1)
                        val normalizedY = (stat.amount - minAmount).toFloat() / range.toFloat()
                        val y = chartTop + chartHeight * (1 - normalizedY)
                        Offset(x, y)
                    }

                    val pathPoints = points.map { Offset(it.x, it.y) }
                    pathPoints.zipWithNext().forEach { (start, end) ->
                        drawLine(
                            color = KeacsColors.Primary,
                            start = start,
                            end = end,
                            strokeWidth = 2.5f,
                        )
                    }

                    points.forEach { point ->
                        drawCircle(
                            color = KeacsColors.Primary,
                            radius = 4f,
                            center = point,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val labelCount = minOf(5, dailyTrend.size)
            val step = (dailyTrend.size - 1) / (labelCount - 1).coerceAtLeast(1)
            repeat(labelCount) { i ->
                val index = (i * step).coerceIn(0, dailyTrend.size - 1)
                Text(
                    text = "${dailyTrend.getOrNull(index)?.day ?: ""}",
                    color = KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CategoryChartCard(
    categoryStats: List<CategoryStats>,
    tab: StatsTab,
) {
    KeacsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = if (tab == StatsTab.EXPENSE) "支出分类" else "收入分类",
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (categoryStats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无数据",
                        color = KeacsColors.TextTertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    PieChart(
                        categoryStats = categoryStats,
                        modifier = Modifier.size(100.dp),
                    )

                    Column(
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categoryStats.take(5).forEach { stat ->
                            CategoryStatRow(stat = stat)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PieChart(
    categoryStats: List<CategoryStats>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        var startAngle = -90f
        val total = categoryStats.sumOf { it.amount.toDouble() }.toFloat()

        categoryStats.forEach { stat ->
            val sweepAngle = (stat.amount / total) * 360f
            drawArc(
                color = colorFor(stat.colorKey),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = Size(size.minDimension, size.minDimension),
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun CategoryStatRow(stat: CategoryStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(colorFor(stat.colorKey)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconFor(stat.iconKey),
                contentDescription = null,
                tint = KeacsColors.Surface,
                modifier = Modifier.size(14.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = stat.categoryName,
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = formatCent(stat.amount),
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = StatsViewModel.formatPercentage(stat.percentage),
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun AssetLiabilityCard(
    totalAsset: Long,
    totalLiability: Long,
    netAsset: Long,
) {
    KeacsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "资产负债",
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                AssetStatItem(
                    label = "总资产",
                    amount = totalAsset,
                    color = KeacsColors.Income,
                )
                AssetStatItem(
                    label = "总负债",
                    amount = totalLiability,
                    color = KeacsColors.Expense,
                )
                AssetStatItem(
                    label = "净资产",
                    amount = netAsset,
                    color = if (netAsset >= 0) KeacsColors.Primary else KeacsColors.Expense,
                )
            }
        }
    }
}

@Composable
private fun AssetStatItem(
    label: String,
    amount: Long,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatCent(amount),
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AccountBalanceListCard(
    accountBalances: List<AccountBalanceStats>,
) {
    KeacsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "账户余额",
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (accountBalances.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无账户",
                        color = KeacsColors.TextTertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                accountBalances.forEachIndexed { index, account ->
                    AccountBalanceRow(account = account)
                    if (index < accountBalances.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountBalanceRow(account: AccountBalanceStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
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

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.accountName,
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = if (account.isAsset) "资产" else "负债",
                color = KeacsColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall,
            )
        }

        Text(
            text = formatCent(account.balance),
            color = if (account.isAsset) KeacsColors.TextPrimary else KeacsColors.Expense,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun NetAssetTrendCard(
    monthlyTrend: List<MonthlyNetAssetStats>,
) {
    KeacsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "净资产变化趋势",
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (monthlyTrend.isEmpty() || monthlyTrend.all { it.netAsset == 0L }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无数据",
                        color = KeacsColors.TextTertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                NetAssetTrendChart(
                    monthlyTrend = monthlyTrend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp),
                )
            }
        }
    }
}

@Composable
private fun NetAssetTrendChart(
    monthlyTrend: List<MonthlyNetAssetStats>,
    modifier: Modifier = Modifier,
) {
    val gridColor = KeacsColors.Border.copy(alpha = 0.75f)
    val maxAmount = monthlyTrend.maxOfOrNull { kotlin.math.abs(it.netAsset) } ?: 1L
    val minAmount = monthlyTrend.minOfOrNull { it.netAsset } ?: 0L
    val range = (maxAmount - minAmount).coerceAtLeast(1L)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.width(32.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatShort(maxAmount),
                    color = KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = formatShort((maxAmount + minAmount) / 2),
                    color = KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = formatShort(minAmount),
                    color = KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(128.dp),
            ) {
                val chartHeight = size.height - 24.dp.toPx()
                val chartTop = 0f

                listOf(0f, 0.5f, 1f).forEach { ratio ->
                    val y = chartTop + chartHeight * ratio
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                }

                if (monthlyTrend.isNotEmpty()) {
                    val points = monthlyTrend.mapIndexed { index, stat ->
                        val x = size.width * index / (monthlyTrend.size - 1).coerceAtLeast(1)
                        val normalizedY = (stat.netAsset - minAmount).toFloat() / range.toFloat()
                        val y = chartTop + chartHeight * (1 - normalizedY)
                        Offset(x, y)
                    }

                    val pathPoints = points.map { Offset(it.x, it.y) }
                    pathPoints.zipWithNext().forEach { (start, end) ->
                        drawLine(
                            color = KeacsColors.Primary,
                            start = start,
                            end = end,
                            strokeWidth = 2.5f,
                        )
                    }

                    points.forEach { point ->
                        drawCircle(
                            color = KeacsColors.Primary,
                            radius = 4f,
                            center = point,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val labelCount = minOf(6, monthlyTrend.size)
            val step = (monthlyTrend.size - 1) / (labelCount - 1).coerceAtLeast(1)
            repeat(labelCount) { i ->
                val index = (i * step).coerceIn(0, monthlyTrend.size - 1)
                Text(
                    text = "${monthlyTrend.getOrNull(index)?.month ?: ""}",
                    color = KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun formatCent(value: Long): String =
    "¥" + DecimalFormat("#,##0.00").format(value / 100.0)

private fun formatShort(value: Long): String {
    return when {
        value >= 1_000_000_00 -> String.format("%.1fW", value / 100_000_000.0)
        value >= 1_000_00 -> String.format("%.1fW", value / 10_000_000.0)
        value >= 1_000_00 -> String.format("%.0f", value / 100_0000.0)
        else -> "¥${value / 100}"
    }
}
