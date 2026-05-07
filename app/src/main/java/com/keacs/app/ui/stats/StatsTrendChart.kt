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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
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
        mutableIntStateOf(dailyTrend.lastIndex)
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
        SelectedChartValueLabel(
            text = StatsViewModel.formatCent(selected.amount),
            color = lineColor,
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
                            selectedIndex = ((offset.x / width) * dailyTrend.size)
                                .toInt()
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
                    val x = chartXForIndex(index, dailyTrend.size, size.width)
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

        ChartXAxisLabels(
            dailyTrend = dailyTrend,
            period = period,
            selectedIndex = currentSelectedIndex,
            selectedColor = lineColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp),
        )
    }
}

@Composable
private fun SelectedChartValueLabel(
    text: String,
    color: Color,
    selectedIndex: Int,
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier,
        content = {
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(color.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        },
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        val width = constraints.maxWidth
        val axisWidth = 48.dp.roundToPx()
        val chartWidth = (width - axisWidth).coerceAtLeast(1)
        val centerX = axisWidth + chartXForIndex(selectedIndex, itemCount, chartWidth.toFloat())
        val x = (centerX - placeable.width / 2f)
            .roundToInt()
            .coerceIn(0, (width - placeable.width).coerceAtLeast(0))

        layout(width, placeable.height) {
            placeable.placeRelative(x, 0)
        }
    }
}

@Composable
internal fun ChartXAxisLabels(
    dailyTrend: List<DailyStats>,
    period: TimePeriod,
    selectedIndex: Int,
    selectedColor: Color,
    modifier: Modifier = Modifier,
) {
    val labelIndices = axisLabelIndices(dailyTrend.size)
    Layout(
        modifier = modifier,
        content = {
            labelIndices.forEach { index ->
                Text(
                    text = axisLabel(dailyTrend[index].day, period),
                    color = if (index == selectedIndex) selectedColor else KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (index == selectedIndex) FontWeight.Medium else FontWeight.Normal,
                )
            }
        },
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { measurable -> measurable.measure(childConstraints) }
        val width = constraints.maxWidth
        val height = placeables.maxOfOrNull { it.height } ?: 0

        layout(width, height) {
            placeables.forEachIndexed { childIndex, placeable ->
                val dataIndex = labelIndices[childIndex]
                val centerX = chartXForIndex(dataIndex, dailyTrend.size, width.toFloat())
                val x = (centerX - placeable.width / 2f)
                    .roundToInt()
                    .coerceIn(0, (width - placeable.width).coerceAtLeast(0))
                placeable.placeRelative(x, 0)
            }
        }
    }
}

internal fun axisLabelIndices(size: Int): List<Int> {
    val labelCount = minOf(6, size)
    return (0 until labelCount).map { index ->
        if (labelCount == 1) {
            0
        } else {
            (index * (size - 1) / (labelCount - 1)).coerceIn(0, size - 1)
        }
    }.distinct()
}

internal fun formatShort(value: Long): String {
    val rmb = value / 100.0
    return when {
        abs(rmb) >= 10000.0 -> String.format(Locale.getDefault(), "%.1f万", rmb / 10000.0)
        else -> String.format(Locale.getDefault(), "%.0f", rmb)
    }
}

internal fun axisLabel(value: Int, period: TimePeriod): String =
    if (period == TimePeriod.YEAR) "${value}月" else "${value}日"

internal fun chartXForIndex(index: Int, size: Int, width: Float): Float =
    if (size <= 0) 0f else width * (index + 0.5f) / size.toFloat()
