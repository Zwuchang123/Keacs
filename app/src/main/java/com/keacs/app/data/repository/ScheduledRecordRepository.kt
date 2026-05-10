package com.keacs.app.data.repository

import com.keacs.app.data.local.database.KeacsDatabase
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.local.entity.ScheduledRecordEntity
import com.keacs.app.domain.model.RecordType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Locale

object ScheduledFrequency {
    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val YEARLY = "YEARLY"
}

class ScheduledRecordRepository(
    private val database: KeacsDatabase,
    private val localDataRepository: LocalDataRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    fun observeSchedules(): Flow<List<ScheduledRecordEntity>> =
        database.scheduledRecordDao().observeAll()

    suspend fun getSchedules(): List<ScheduledRecordEntity> =
        database.scheduledRecordDao().getAll()

    suspend fun getSchedule(id: Long): ScheduledRecordEntity? =
        database.scheduledRecordDao().getById(id)

    suspend fun saveSchedule(
        id: Long?,
        type: String,
        amountCent: Long,
        categoryId: Long?,
        fromAccountId: Long?,
        toAccountId: Long?,
        frequency: String,
        recurrenceValues: String? = null,
        nextRunAt: Long,
        note: String,
        isEnabled: Boolean,
    ) {
        val normalizedFrequency = normalizeFrequency(frequency)
        validateSchedule(type, amountCent, categoryId, fromAccountId, toAccountId, normalizedFrequency)
        val now = clock()
        val recurrence = recurrenceFromDate(normalizedFrequency, nextRunAt, recurrenceValues)
        val normalizedNextRunAt = nextOccurrence(
            frequency = normalizedFrequency,
            month = recurrence.month,
            day = recurrence.day,
            weekday = recurrence.weekday,
            recurrenceValues = recurrence.values,
            hour = recurrence.hour,
            afterMillis = nextRunAt - 1,
        )
        val normalizedCategoryId = if (type == RecordType.TRANSFER) null else categoryId
        val normalizedFromAccountId = if (type == RecordType.INCOME) null else fromAccountId
        val normalizedToAccountId = if (type == RecordType.EXPENSE) null else toAccountId
        if (id == null) {
            database.scheduledRecordDao().insert(
                ScheduledRecordEntity(
                    type = type,
                    amountCent = amountCent,
                    categoryId = normalizedCategoryId,
                    fromAccountId = normalizedFromAccountId,
                    toAccountId = normalizedToAccountId,
                    frequency = normalizedFrequency,
                    recurrenceMonth = recurrence.month,
                    recurrenceDay = recurrence.day,
                    recurrenceWeekday = recurrence.weekday,
                    recurrenceValues = recurrence.values,
                    recurrenceHour = recurrence.hour,
                    nextRunAt = normalizedNextRunAt,
                    note = note.trim().ifBlank { null },
                    isEnabled = isEnabled,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            val old = requireNotNull(database.scheduledRecordDao().getById(id)) { "定时记账不存在" }
            database.scheduledRecordDao().update(
                old.copy(
                    type = type,
                    amountCent = amountCent,
                    categoryId = normalizedCategoryId,
                    fromAccountId = normalizedFromAccountId,
                    toAccountId = normalizedToAccountId,
                    frequency = normalizedFrequency,
                    recurrenceMonth = recurrence.month,
                    recurrenceDay = recurrence.day,
                    recurrenceWeekday = recurrence.weekday,
                    recurrenceValues = recurrence.values,
                    recurrenceHour = recurrence.hour,
                    nextRunAt = normalizedNextRunAt,
                    note = note.trim().ifBlank { null },
                    isEnabled = isEnabled,
                    updatedAt = now,
                ),
            )
        }
    }

    suspend fun deleteSchedule(id: Long) {
        database.scheduledRecordDao().deleteById(id)
    }

    suspend fun createDueRecords(): Int {
        val now = clock()
        var created = 0
        database.scheduledRecordDao().getAll()
            .filter { it.isEnabled && it.nextRunAt <= now }
            .forEach { schedule ->
                var nextRunAt = schedule.nextRunAt
                var guard = 0
                var failed = false
                while (nextRunAt <= now && guard < MAX_CREATE_PER_SCHEDULE && !failed) {
                    runCatching {
                        createRecordFromSchedule(schedule, nextRunAt)
                    }.onFailure {
                        failed = true
                    }
                    if (failed) break
                    created += 1
                    nextRunAt = nextDate(schedule, nextRunAt)
                    guard += 1
                }
                database.scheduledRecordDao().update(
                    schedule.copy(nextRunAt = nextRunAt, isEnabled = !failed, updatedAt = clock()),
                )
            }
        return created
    }

    private suspend fun createRecordFromSchedule(schedule: ScheduledRecordEntity, occurredAt: Long) {
        when (schedule.type) {
            RecordType.INCOME -> localDataRepository.saveRecord(
                id = null,
                type = RecordType.INCOME,
                amountCent = schedule.amountCent,
                occurredAt = occurredAt,
                categoryId = requireNotNull(schedule.categoryId) { "请选择分类" },
                fromAccountId = null,
                toAccountId = schedule.toAccountId,
                note = schedule.note,
            )
            RecordType.EXPENSE -> localDataRepository.saveRecord(
                id = null,
                type = RecordType.EXPENSE,
                amountCent = schedule.amountCent,
                occurredAt = occurredAt,
                categoryId = requireNotNull(schedule.categoryId) { "请选择分类" },
                fromAccountId = schedule.fromAccountId,
                toAccountId = null,
                note = schedule.note,
            )
            RecordType.TRANSFER -> localDataRepository.saveRecord(
                id = null,
                type = RecordType.TRANSFER,
                amountCent = schedule.amountCent,
                occurredAt = occurredAt,
                categoryId = null,
                fromAccountId = schedule.fromAccountId,
                toAccountId = schedule.toAccountId,
                note = schedule.note,
            )
            else -> error("账目类型不正确")
        }
    }

    private suspend fun validateSchedule(
        type: String,
        amountCent: Long,
        categoryId: Long?,
        fromAccountId: Long?,
        toAccountId: Long?,
        frequency: String,
    ) {
        require(amountCent > 0) { "金额大于0才可保存" }
        require(frequency in allowedFrequencies) { "重复周期不正确" }
        if (type == RecordType.TRANSFER) {
            require(fromAccountId != null) { "请选择转出账户" }
            require(toAccountId != null) { "请选择转入账户" }
            require(fromAccountId != toAccountId) { "转出和转入账户不能相同" }
            require(database.accountDao().getById(fromAccountId) != null) { "转出账户不存在" }
            require(database.accountDao().getById(toAccountId) != null) { "转入账户不存在" }
            return
        }
        require(type == RecordType.INCOME || type == RecordType.EXPENSE) { "账目类型不正确" }
        val category = requireNotNull(categoryId?.let { database.categoryDao().getById(it) }) { "请选择分类" }
        val expectedDirection = if (type == RecordType.INCOME) {
            PresetSeedData.CATEGORY_INCOME
        } else {
            PresetSeedData.CATEGORY_EXPENSE
        }
        require(category.direction == expectedDirection) { "分类类型不正确" }
        val accountId = if (type == RecordType.INCOME) toAccountId else fromAccountId
        accountId?.let { require(database.accountDao().getById(it) != null) { "账户不存在" } }
    }

    companion object {
        private const val MAX_CREATE_PER_SCHEDULE = 370
        const val DEFAULT_RECURRENCE_HOUR = 9
        private val allowedFrequencies = setOf(
            ScheduledFrequency.WEEKLY,
            ScheduledFrequency.MONTHLY,
            ScheduledFrequency.YEARLY,
        )

        fun startOfDay(timeMillis: Long): Long =
            Calendar.getInstance(Locale.getDefault()).apply {
                timeInMillis = timeMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

        fun normalizeFrequency(frequency: String): String =
            if (frequency == ScheduledFrequency.DAILY) ScheduledFrequency.WEEKLY else frequency

        fun recurrenceFromDate(
            frequency: String,
            timeMillis: Long,
            recurrenceValues: String? = null,
        ): ScheduledRecurrence {
            val normalizedFrequency = normalizeFrequency(frequency)
            val calendar = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = timeMillis }
            val values = recurrenceValues?.takeIf { it.isNotBlank() }
                ?: recurrenceValuesFromParts(
                    frequency = normalizedFrequency,
                    month = if (normalizedFrequency == ScheduledFrequency.YEARLY) {
                        calendar.get(Calendar.MONTH) + 1
                    } else {
                        null
                    },
                    day = when (normalizedFrequency) {
                        ScheduledFrequency.MONTHLY, ScheduledFrequency.YEARLY -> calendar.get(Calendar.DAY_OF_MONTH)
                        else -> null
                    },
                    weekday = if (normalizedFrequency == ScheduledFrequency.WEEKLY) {
                        calendar.get(Calendar.DAY_OF_WEEK)
                    } else {
                        null
                    },
                    fallbackMillis = timeMillis,
                )
            val firstValue = values.substringBefore(",")
            val firstYearlyValue = firstValue.split("-")
            return ScheduledRecurrence(
                month = if (normalizedFrequency == ScheduledFrequency.YEARLY) {
                    firstYearlyValue.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.MONTH) + 1
                } else {
                    null
                },
                day = when (normalizedFrequency) {
                    ScheduledFrequency.MONTHLY -> firstValue.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)
                    ScheduledFrequency.YEARLY -> firstYearlyValue.getOrNull(1)?.toIntOrNull()
                        ?: calendar.get(Calendar.DAY_OF_MONTH)
                    else -> null
                },
                weekday = if (normalizedFrequency == ScheduledFrequency.WEEKLY) {
                    firstValue.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_WEEK)
                } else {
                    null
                },
                values = values,
                hour = DEFAULT_RECURRENCE_HOUR,
            )
        }

        fun recurrenceValuesFromParts(
            frequency: String,
            month: Int?,
            day: Int?,
            weekday: Int?,
            fallbackMillis: Long,
        ): String {
            val normalizedFrequency = normalizeFrequency(frequency)
            val calendar = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = fallbackMillis }
            return when (normalizedFrequency) {
                ScheduledFrequency.WEEKLY -> listOf(weekday ?: calendar.get(Calendar.DAY_OF_WEEK))
                    .joinToString(",")
                ScheduledFrequency.YEARLY -> listOf(
                    "${month ?: calendar.get(Calendar.MONTH) + 1}-${day ?: calendar.get(Calendar.DAY_OF_MONTH)}",
                ).joinToString(",")
                else -> listOf(day ?: calendar.get(Calendar.DAY_OF_MONTH)).joinToString(",")
            }
        }

        fun nextDate(schedule: ScheduledRecordEntity, fromMillis: Long): Long =
            nextOccurrence(
                frequency = schedule.frequency,
                month = schedule.recurrenceMonth,
                day = schedule.recurrenceDay,
                weekday = schedule.recurrenceWeekday,
                recurrenceValues = schedule.recurrenceValues,
                hour = schedule.recurrenceHour,
                afterMillis = fromMillis,
            )

        fun nextOccurrence(
            frequency: String,
            month: Int?,
            day: Int?,
            weekday: Int?,
            recurrenceValues: String? = null,
            hour: Int,
            afterMillis: Long,
        ): Long {
            val base = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = afterMillis }
            val normalizedFrequency = normalizeFrequency(frequency)
            val safeHour = hour.takeIf { it in 0..23 } ?: DEFAULT_RECURRENCE_HOUR
            return when (normalizedFrequency) {
                ScheduledFrequency.WEEKLY -> {
                    val weekdays = parseIntValues(recurrenceValues, weekday ?: base.get(Calendar.DAY_OF_WEEK))
                        .map { it.coerceIn(Calendar.SUNDAY, Calendar.SATURDAY) }
                    weekdays.minOf { weeklyOccurrence(base, it, safeHour) }
                }
                ScheduledFrequency.MONTHLY -> {
                    val days = parseIntValues(recurrenceValues, day ?: base.get(Calendar.DAY_OF_MONTH))
                        .map { it.coerceIn(1, 31) }
                    days.minOf { monthlyOccurrence(base, it, safeHour) }
                }
                else -> {
                    val yearlyValues = parseYearlyValues(
                        recurrenceValues = recurrenceValues,
                        fallbackMonth = month ?: (base.get(Calendar.MONTH) + 1),
                        fallbackDay = day ?: base.get(Calendar.DAY_OF_MONTH),
                    )
                    yearlyValues.minOf { yearly ->
                        yearlyOccurrence(
                            base = base,
                            month = yearly.month,
                            day = yearly.day,
                            hour = safeHour,
                        )
                    }
                }
            }
        }

        private fun weeklyOccurrence(base: Calendar, weekday: Int, hour: Int): Long {
            val candidate = (base.clone() as Calendar).apply {
                set(Calendar.DAY_OF_WEEK, weekday)
                setTimeParts(hour)
            }
            if (candidate.timeInMillis <= base.timeInMillis) candidate.add(Calendar.WEEK_OF_YEAR, 1)
            return candidate.timeInMillis
        }

        private fun parseIntValues(recurrenceValues: String?, fallback: Int): List<Int> {
            val values = recurrenceValues
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.distinct()
                ?.takeIf { it.isNotEmpty() }
            return values ?: listOf(fallback)
        }

        private fun parseYearlyValues(
            recurrenceValues: String?,
            fallbackMonth: Int,
            fallbackDay: Int,
        ): List<YearlyValue> {
            val values = recurrenceValues
                ?.split(",")
                ?.mapNotNull { raw ->
                    val parts = raw.trim().split("-")
                    val valueMonth = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                    val valueDay = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                    YearlyValue(valueMonth.coerceIn(1, 12), valueDay.coerceIn(1, 31))
                }
                ?.distinct()
                ?.takeIf { it.isNotEmpty() }
            return values ?: listOf(YearlyValue(fallbackMonth, fallbackDay))
        }

        private fun monthlyOccurrence(base: Calendar, day: Int, hour: Int): Long {
            val candidate = (base.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, day.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
                setTimeParts(hour)
            }
            if (candidate.timeInMillis <= base.timeInMillis) {
                candidate.set(Calendar.DAY_OF_MONTH, 1) // Prevent month rollover
                candidate.add(Calendar.MONTH, 1)
                candidate.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(candidate.getActualMaximum(Calendar.DAY_OF_MONTH)))
                candidate.setTimeParts(hour)
            }
            return candidate.timeInMillis
        }

        private fun yearlyOccurrence(base: Calendar, month: Int, day: Int, hour: Int): Long {
            val candidate = (base.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1) // Prevent month rollover
                set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
                set(Calendar.DAY_OF_MONTH, day.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
                setTimeParts(hour)
            }
            if (candidate.timeInMillis <= base.timeInMillis) {
                candidate.add(Calendar.YEAR, 1)
                candidate.set(Calendar.DAY_OF_MONTH, 1)
                candidate.set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
                candidate.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(candidate.getActualMaximum(Calendar.DAY_OF_MONTH)))
                candidate.setTimeParts(hour)
            }
            return candidate.timeInMillis
        }

        private fun Calendar.setTimeParts(hour: Int) {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}

data class ScheduledRecurrence(
    val month: Int?,
    val day: Int?,
    val weekday: Int?,
    val values: String,
    val hour: Int,
)

private data class YearlyValue(
    val month: Int,
    val day: Int,
)
