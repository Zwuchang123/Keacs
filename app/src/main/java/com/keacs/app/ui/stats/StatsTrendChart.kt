package com.keacs.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.theme.KeacsColors
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TrendLineChart(
    dailyTrend: List<DailyStats>,
    period: TimePeriod,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    if (dailyTrend.isEmpty()) return

    var selectedIndex by remember(dailyTrend) {
        mutableIntStateOf(dailyTrend.indexOfLast { it.amount != 0L }.takeIf { it >= 0 } ?: dailyTrend.lastIndex)
    }
    val currentSelectedIndex = selectedIndex.coerceIn(0, dailyTrend.lastIndex)
    val selected = dailyTrend[currentSelectedIndex]
    val maxValue = dailyTrend.maxOf { it.amount }
    val minValue = dailyTrend.minOf { it.amount }
    val axisMax = maxOf(maxValue, 0L)
    val axisMin = minOf(minValue, 0L)
    val visualMax = if (axisMax == axisMin) axisMax + 100L else axisMax
    val visualMin = axisMin
    val visualRange = (visualMax - visualMin).coerceAtLeast(1L)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = axisLabel(selected.day, period),
                color = KeacsColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = StatsViewModel.formatCent(selected.amount),
                color = lineColor,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(lineColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .height(126.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatShort(axisMax), color = KeacsColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
                Text(formatShort((axisMax + axisMin) / 2), color = KeacsColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
                Text(formatShort(axisMin), color = KeacsColors.TextTertiary, style = MaterialTheme.typography.bodySmall)
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(126.dp)
                    .pointerInput(dailyTrend) {
                        detectTapGestures { offset ->
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            selectedIndex = ((offset.x / width) * dailyTrend.lastIndex)
                                .roundToInt()
                                .coerceIn(0, dailyTrend.lastIndex)
                        }
                    },
            ) {
                val chartTop = 8.dp.toPx()
                val chartBottom = size.height - 10.dp.toPx()
                val chartHeight = chartBottom - chartTop
                val gridColor = KeacsColors.Border.copy(alpha = 0.75f)

                listOf(0f, 0.5f, 1f).forEach { ratio ->
                    val y = chartTop + chartHeight * ratio
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }

                val points = dailyTrend.mapIndexed { index, stat ->
                    val x = size.width * index / dailyTrend.lastIndex.coerceAtLeast(1)
                    val normalizedY = (stat.amount - visualMin).toFloat() / visualRange.toFloat()
                    Offset(x, chartTop + chartHeight * (1f - normalizedY))
                }

                points.zipWithNext().forEach { (start, end) ->
                    drawLine(lineColor, start, end, strokeWidth = 3f)
                }

                val selectedPoint = points[currentSelectedIndex]
                drawLine(
                    color = lineColor.copy(alpha = 0.24f),
                    start = Offset(selectedPoint.x, chartTop),
                    end = Offset(selectedPoint.x, chartBottom),
                    strokeWidth = 1.5f,
                )

                points.forEachIndexed { index, point ->
                    val isSelected = index == currentSelectedIndex
                    drawCircle(
                        color = if (isSelected) KeacsColors.Surface else lineColor,
                        radius = if (isSelected) 6.5f else 3.5f,
                        center = point,
                    )
                    drawCircle(
                        color = lineColor,
                        radius = if (isSelected) 4.8f else 3.5f,
                        center = point,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            axisLabelIndices(dailyTrend.size).forEach { index ->
                Text(
                    text = axisLabel(dailyTrend[index].day, period),
                    color = if (index == currentSelectedIndex) lineColor else KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (index == currentSelectedIndex) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

private fun axisLabelIndices(size: Int): List<Int> {
    val labelCount = minOf(6, size)
    return (0 until labelCount).map { index ->
        if (labelCount == 1) {
            0
        } else {
            (index * (size - 1) / (labelCount - 1)).coerceIn(0, size - 1)
        }
    }.distinct()
}

private fun formatShort(value: Long): String {
    val rmb = value / 100.0
    return when {
        abs(rmb) >= 10000.0 -> String.format(Locale.getDefault(), "%.1f万", rmb / 10000.0)
        else -> String.format(Locale.getDefault(), "%.0f", rmb)
    }
}

private fun axisLabel(value: Int, period: TimePeriod): String =
    if (period == TimePeriod.YEAR) "${value}月" else "${value}日"
