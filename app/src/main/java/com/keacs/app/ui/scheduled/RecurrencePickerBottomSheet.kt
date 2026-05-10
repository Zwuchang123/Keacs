package com.keacs.app.ui.scheduled

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.keacs.app.data.repository.ScheduledFrequency
import com.keacs.app.data.repository.ScheduledRecordRepository
import com.keacs.app.ui.components.WheelPickerBottomSheet
import com.keacs.app.ui.components.WheelPickerColumn
import java.util.Calendar
import java.util.Locale

@Composable
fun RecurrencePickerBottomSheet(
    frequency: String,
    nextRunAt: Long,
    onSelected: (String, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val initial = remember(nextRunAt) {
        Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = nextRunAt }
    }
    var selectedFrequency by remember(frequency) { mutableStateOf(frequency) }
    var monthIndex by remember(nextRunAt) { mutableIntStateOf(initial.get(Calendar.MONTH)) }
    var dayIndex by remember(nextRunAt) { mutableIntStateOf((initial.get(Calendar.DAY_OF_MONTH) - 1).coerceIn(0, 30)) }
    var weekdayIndex by remember(nextRunAt) {
        mutableIntStateOf(weekdayValues.indexOf(initial.get(Calendar.DAY_OF_WEEK)).coerceAtLeast(0))
    }
    var hourIndex by remember(nextRunAt) { mutableIntStateOf(initial.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)) }

    val frequencyIndex = frequencyValues.indexOf(selectedFrequency).coerceAtLeast(0)
    val columns = buildList {
        add(WheelPickerColumn(frequencyLabels, frequencyIndex) { selectedFrequency = frequencyValues[it] })
        when (selectedFrequency) {
            ScheduledFrequency.YEARLY -> {
                add(WheelPickerColumn(monthLabels, monthIndex) { monthIndex = it })
                add(WheelPickerColumn(dayLabels, dayIndex) { dayIndex = it })
            }
            ScheduledFrequency.MONTHLY -> add(WheelPickerColumn(dayLabels, dayIndex) { dayIndex = it })
            ScheduledFrequency.WEEKLY -> add(WheelPickerColumn(weekdayLabels, weekdayIndex) { weekdayIndex = it })
        }
        add(WheelPickerColumn(hourLabels, hourIndex) { hourIndex = it })
    }

    WheelPickerBottomSheet(
        title = "生成时间",
        columns = columns,
        onDismiss = onDismiss,
        onConfirm = {
            val nextTime = ScheduledRecordRepository.nextOccurrence(
                frequency = selectedFrequency,
                month = monthIndex + 1,
                day = dayIndex + 1,
                weekday = weekdayValues[weekdayIndex.coerceIn(weekdayValues.indices)],
                hour = hourIndex,
                afterMillis = System.currentTimeMillis(),
            )
            onSelected(selectedFrequency, nextTime)
        },
    )
}

fun recurrenceLabel(frequency: String, nextRunAt: Long): String {
    val calendar = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = nextRunAt }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    return when (frequency) {
        ScheduledFrequency.YEARLY -> "每年${calendar.get(Calendar.MONTH) + 1}月${calendar.get(Calendar.DAY_OF_MONTH)}日 ${hour}点"
        ScheduledFrequency.WEEKLY -> "${weekdayLabels[weekdayValues.indexOf(calendar.get(Calendar.DAY_OF_WEEK)).coerceAtLeast(0)]} ${hour}点"
        ScheduledFrequency.DAILY -> "每天 ${hour}点"
        else -> "每月${calendar.get(Calendar.DAY_OF_MONTH)}日 ${hour}点"
    }
}

private val frequencyValues = listOf(
    ScheduledFrequency.DAILY,
    ScheduledFrequency.WEEKLY,
    ScheduledFrequency.MONTHLY,
    ScheduledFrequency.YEARLY,
)
private val frequencyLabels = listOf("每天", "每周", "每月", "每年")
private val monthLabels = (1..12).map { "${it}月" }
private val dayLabels = (1..31).map { "${it}日" }
private val hourLabels = (0..23).map { "${it}点" }
private val weekdayValues = listOf(
    Calendar.MONDAY,
    Calendar.TUESDAY,
    Calendar.WEDNESDAY,
    Calendar.THURSDAY,
    Calendar.FRIDAY,
    Calendar.SATURDAY,
    Calendar.SUNDAY,
)
private val weekdayLabels = listOf("每周一", "每周二", "每周三", "每周四", "每周五", "每周六", "每周日")
