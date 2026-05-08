package com.keacs.app.ui.home

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.NorthEast
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.record.DatePickerMode
import com.keacs.app.ui.record.DateWheelPickerBottomSheet
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onRecordClick: (Long) -> Unit,
    onSwipeLeft: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMonthPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-home")
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        totalDrag += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        if (totalDrag <= -60f) {
                            onSwipeLeft()
                        }
                        totalDrag = 0f
                    }
                )
            }
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        OverviewCard(
            selectedMonth = uiState.selectedMonth,
            totalIncome = uiState.totalIncome,
            totalExpense = uiState.totalExpense,
            onMonthClick = { showMonthPicker = true },
        )
        MonthlyBillSection(
            groups = uiState.dailyGroups,
            categories = uiState.categories,
            accounts = uiState.accounts,
            onRecordClick = onRecordClick,
            modifier = Modifier.weight(1f),
        )
    }

    if (showMonthPicker) {
        DateWheelPickerBottomSheet(
            title = "选择月份",
            selectedDate = uiState.selectedMonth,
            mode = DatePickerMode.MONTH,
            onSelected = {
                viewModel.selectMonth(it)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false },
        )
    }
}

@Composable
private fun OverviewCard(
    selectedMonth: Long,
    totalIncome: Long,
    totalExpense: Long,
    onMonthClick: () -> Unit,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onMonthClick),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = formatMonthText(selectedMonth),
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
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "选择月份",
                    tint = KeacsColors.PrimaryLight,
                )
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
private fun Modifier.clipLarge(): Modifier = clip(MaterialTheme.shapes.large)

private fun formatMonthText(timestamp: Long): String =
    SimpleDateFormat("yyyy年MM月", Locale.getDefault()).format(Date(timestamp))
