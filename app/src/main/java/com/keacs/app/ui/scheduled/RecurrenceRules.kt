package com.keacs.app.ui.scheduled

import com.keacs.app.data.local.entity.ScheduledRecordEntity
import com.keacs.app.data.repository.ScheduledFrequency
import com.keacs.app.data.repository.ScheduledRecordRepository
import java.util.Calendar
import java.util.Locale

fun recurrenceLabel(schedule: ScheduledRecordEntity): String =
    recurrenceLabel(
        frequency = schedule.frequency,
        recurrenceValues = schedule.recurrenceValues
            ?: ScheduledRecordRepository.recurrenceValuesFromParts(
                frequency = schedule.frequency,
                month = schedule.recurrenceMonth,
                day = schedule.recurrenceDay,
                weekday = schedule.recurrenceWeekday,
                fallbackMillis = schedule.nextRunAt,
            ),
        nextRunAt = schedule.nextRunAt,
    )

fun recurrenceLabel(frequency: String, recurrenceValues: String?, nextRunAt: Long): String {
    val calendar = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = nextRunAt }
    val normalizedFrequency = ScheduledRecordRepository.normalizeFrequency(frequency)
    val values = recurrenceValues ?: ScheduledRecordRepository.recurrenceValuesFromParts(
        frequency = normalizedFrequency,
        month = if (normalizedFrequency == ScheduledFrequency.YEARLY) calendar.get(Calendar.MONTH) + 1 else null,
        day = if (normalizedFrequency != ScheduledFrequency.WEEKLY) calendar.get(Calendar.DAY_OF_MONTH) else null,
        weekday = if (normalizedFrequency == ScheduledFrequency.WEEKLY) calendar.get(Calendar.DAY_OF_WEEK) else null,
        fallbackMillis = nextRunAt,
    )
    return when (normalizedFrequency) {
        ScheduledFrequency.WEEKLY -> {
            val labels = parseWeekdays(values, calendar.get(Calendar.DAY_OF_WEEK)).map { weekdayName(it) }
            "每${labels.joinToString("、")} 09:00"
        }
        ScheduledFrequency.YEARLY -> {
            val labels = parseYearlyValues(
                values = values,
                fallbackMonth = calendar.get(Calendar.MONTH) + 1,
                fallbackDay = calendar.get(Calendar.DAY_OF_MONTH),
            ).map { "${it.month}月${it.day}日" }
            "每年${labels.joinToString("、")} 09:00"
        }
        else -> {
            val labels = parseMonthDays(values, calendar.get(Calendar.DAY_OF_MONTH)).map { "${it}日" }
            "每月${labels.joinToString("、")} 09:00"
        }
    }
}

internal fun parseWeekdays(values: String?, fallback: Int): List<Int> =
    parseIntValues(values, fallback)
        .filter { it in Calendar.SUNDAY..Calendar.SATURDAY }
        .distinct()
        .sortedWith(compareBy { weekdayValues.indexOf(it).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE })

internal fun parseMonthDays(values: String?, fallback: Int): List<Int> =
    parseIntValues(values, fallback)
        .map { it.coerceIn(1, 31) }
        .distinct()
        .sorted()

private fun parseIntValues(values: String?, fallback: Int): List<Int> =
    values
        ?.split(",")
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?.takeIf { it.isNotEmpty() }
        ?: listOf(fallback)

internal fun parseYearlyValues(values: String?, fallbackMonth: Int, fallbackDay: Int): List<YearlyValue> =
    values
        ?.split(",")
        ?.mapNotNull { raw ->
            val parts = raw.trim().split("-")
            val month = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val day = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            YearlyValue(month.coerceIn(1, 12), day.coerceIn(1, 31))
        }
        ?.distinct()
        ?.sortedWith(compareBy<YearlyValue> { it.month }.thenBy { it.day })
        ?.takeIf { it.isNotEmpty() }
        ?: listOf(YearlyValue(fallbackMonth.coerceIn(1, 12), fallbackDay.coerceIn(1, 31)))

private fun weekdayName(value: Int): String =
    weekdayLabels[weekdayValues.indexOf(value).coerceAtLeast(0)]

internal val frequencyValues = listOf(
    ScheduledFrequency.WEEKLY,
    ScheduledFrequency.MONTHLY,
    ScheduledFrequency.YEARLY,
)
internal val frequencyLabels = listOf("每周", "每月", "每年")
internal val weekdayValues = listOf(
    Calendar.MONDAY,
    Calendar.TUESDAY,
    Calendar.WEDNESDAY,
    Calendar.THURSDAY,
    Calendar.FRIDAY,
    Calendar.SATURDAY,
    Calendar.SUNDAY,
)
internal val weekdayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

internal data class YearlyValue(
    val month: Int,
    val day: Int,
)
