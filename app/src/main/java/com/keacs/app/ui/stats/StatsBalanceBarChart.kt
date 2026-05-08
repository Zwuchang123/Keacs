package com.keacs.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.theme.KeacsColors
import kotlin.math.abs

@Composable
fun BalanceBarChart(
    dailyTrend: List<DailyStats>,
    period: TimePeriod,
    positiveColor: Color,
    negativeColor: Color,
    modifier: Modifier = Modifier,
) {
    if (dailyTrend.isEmpty()) return

    var selectedIndex by remember(dailyTrend) { mutableIntStateOf(dailyTrend.lastIndex) }
    val currentSelectedIndex = selectedIndex.coerceIn(0, dailyTrend.lastIndex)
    val selected = dailyTrend[currentSelectedIndex]
    val selectedColor = balanceColor(selected.amount, positiveColor, negativeColor)
    val axisMaxValue = dailyTrend.maxOf { abs(it.amount) }
    val visualMaxValue = axisMaxValue.coerceAtLeast(100L)

    Column(modifier = modifier) {
        SelectedChartValueLabel(
            text = StatsViewModel.formatCent(selected.amount),
            color = selectedColor,
            selectedIndex = currentSelectedIndex,
            itemCount = dailyTrend.size,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .height(126.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatShort(axisMaxValue), color = KeacsColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
                Text(formatShort(axisMaxValue / 2), color = KeacsColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
                Text(formatShort(0), color = KeacsColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(126.dp)
                    .pointerInput(dailyTrend) {
                        detectTapGestures { offset ->
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            selectedIndex = ((offset.x / width) * dailyTrend.size)
                                .toInt()
                                .coerceIn(0, dailyTrend.lastIndex)
                        }
                    },
            ) {
                val chartTop = 8.dp.toPx()
                val chartBottom = size.height - 10.dp.toPx()
                val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)
                val gridColor = KeacsColors.Border.copy(alpha = 0.75f)
                val slotWidth = size.width / dailyTrend.size.coerceAtLeast(1)
                val barWidth = (slotWidth * 0.56f).coerceIn(4.dp.toPx(), 18.dp.toPx())
                val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                listOf(0f, 0.5f, 1f).forEach { ratio ->
                    val y = chartTop + chartHeight * ratio
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }

                val selectedCenterX = chartXForIndex(currentSelectedIndex, dailyTrend.size, size.width)
                drawLine(
                    color = selectedColor.copy(alpha = 0.16f),
                    start = Offset(selectedCenterX, chartTop),
                    end = Offset(selectedCenterX, chartBottom),
                    strokeWidth = 1.5f,
                )

                // 负结余只用颜色区分，柱子仍从底部向上增长，避免出现向下柱。
                dailyTrend.forEachIndexed { index, stat ->
                    val valueAbs = abs(stat.amount)
                    val barHeight = if (valueAbs == 0L) {
                        0f
                    } else {
                        (chartHeight * valueAbs.toFloat() / visualMaxValue.toFloat()).coerceAtLeast(3.dp.toPx())
                    }
                    val left = slotWidth * index + (slotWidth - barWidth) / 2f
                    val top = chartBottom - barHeight
                    val barColor = balanceColor(stat.amount, positiveColor, negativeColor)
                    val isSelected = index == currentSelectedIndex

                    if (barHeight > 0f) {
                        drawRoundRect(
                            color = barColor.copy(alpha = if (isSelected) 1f else 0.72f),
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = corner,
                        )
                        if (isSelected) {
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = corner,
                                style = Stroke(width = 1.5.dp.toPx()),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        ChartXAxisLabels(
            dailyTrend = dailyTrend,
            period = period,
            selectedIndex = currentSelectedIndex,
            selectedColor = selectedColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp),
        )
    }
}

private fun balanceColor(value: Long, positiveColor: Color, negativeColor: Color): Color =
    if (value >= 0L) positiveColor else negativeColor
