package com.keacs.app.data.agent

import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.local.entity.RecordEntity
import com.keacs.app.data.local.entity.ScheduledRecordEntity
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.data.repository.ScheduledRecordRepository
import com.keacs.app.domain.model.RecordType
import com.keacs.app.domain.rule.balanceFor
import com.keacs.app.domain.rule.totalExpense
import com.keacs.app.domain.rule.totalIncome
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AgentContextProvider(
    private val repository: LocalDataRepository,
    private val scheduledRepository: ScheduledRecordRepository? = null,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun buildForMessage(message: String): AgentLocalContext {
        val categories = repository.getCategories()
        val accounts = repository.getAccounts()
        val records = repository.getRecords()
        val range = rangeForMessage(message)
        val rangeRecords = records.filter { it.occurredAt in range.start until range.endExclusive }
        val candidateRecords = if (message.needsRecordCandidates()) {
            rangeRecords
                .sortedByDescending { it.occurredAt }
                .take(MAX_RECORD_CANDIDATES)
        } else {
            emptyList()
        }
        val schedules = if (message.needsSchedules()) {
            scheduledRepository?.getSchedules().orEmpty().take(MAX_SCHEDULES)
        } else {
            emptyList()
        }

        return AgentLocalContext(
            categories = categories
                .filter { it.isEnabled && it.direction in accountOrRecordDirections }
                .sortedWith(compareBy<CategoryEntity> { it.direction }.thenBy { it.sortOrder })
                .map { it.toAgentMap() },
            accounts = accounts
                .filter { it.isEnabled }
                .map { it.toAgentMap(records) },
            records = candidateRecords.map { it.toAgentMap(categories, accounts) },
            stats = rangeRecords.toStatsMap(range, categories, accounts, records),
            scheduledRecords = schedules.map { it.toAgentMap(categories, accounts) },
        )
    }

    private fun rangeForMessage(message: String): ContextRange {
        val now = clock()
        val lowerMessage = message.lowercase()
        return when {
            lowerMessage.contains("昨天") || lowerMessage.contains("昨日") -> dayRange(now, -1, "昨天")
            lowerMessage.contains("今天") -> dayRange(now, 0, "今天")
            lowerMessage.contains("最近") || lowerMessage.contains("近7天") || lowerMessage.contains("近七天") ->
                rollingRange(now, 7, "最近7天")
            lowerMessage.contains("上月") || lowerMessage.contains("上个月") -> monthRange(now, -1, "上个月")
            else -> monthRange(now, 0, "本月")
        }
    }

    private fun dayRange(now: Long, offsetDays: Int, label: String): ContextRange {
        val start = Calendar.getInstance(Locale.getDefault()).apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, offsetDays)
            setDayStart()
        }.timeInMillis
        return ContextRange(start, start + DAY_MILLIS, label)
    }

    private fun rollingRange(now: Long, days: Int, label: String): ContextRange {
        val end = Calendar.getInstance(Locale.getDefault()).apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            setDayStart()
        }.timeInMillis
        return ContextRange(end - days * DAY_MILLIS, end, label)
    }

    private fun monthRange(now: Long, offsetMonths: Int, label: String): ContextRange {
        val startCalendar = Calendar.getInstance(Locale.getDefault()).apply {
            timeInMillis = now
            add(Calendar.MONTH, offsetMonths)
            set(Calendar.DAY_OF_MONTH, 1)
            setDayStart()
        }
        val start = startCalendar.timeInMillis
        val end = (startCalendar.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.timeInMillis
        return ContextRange(start, end, label)
    }

    private fun Calendar.setDayStart() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun String.needsRecordCandidates(): Boolean =
        listOf("改", "删", "那笔", "账目", "大额", "最近", "明细", "消费").any { contains(it) }

    private fun String.needsSchedules(): Boolean =
        listOf("定时", "每周", "每月", "每年", "房租").any { contains(it) }

    private fun CategoryEntity.toAgentMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "direction" to direction,
    )

    private fun AccountEntity.toAgentMap(records: List<RecordEntity>): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "nature" to nature,
        "type" to type,
        "balanceCent" to balanceFor(this, records),
    )

    private fun RecordEntity.toAgentMap(
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "type" to type,
        "amountCent" to amountCent,
        "occurredAt" to occurredAt,
        "date" to formatDate(occurredAt),
        "categoryName" to categories.firstOrNull { it.id == categoryId }?.name,
        "fromAccountName" to accounts.firstOrNull { it.id == fromAccountId }?.name,
        "toAccountName" to accounts.firstOrNull { it.id == toAccountId }?.name,
        "note" to note.orEmpty(),
    )

    private fun List<RecordEntity>.toStatsMap(
        range: ContextRange,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>,
        allRecords: List<RecordEntity>,
    ): Map<String, Any?> {
        val categoryNameById = categories.associate { it.id to it.name }
        val accountSummary = accounts
            .filter { it.isEnabled }
            .map {
                mapOf(
                    "id" to it.id,
                    "name" to it.name,
                    "nature" to it.nature,
                    "balanceCent" to balanceFor(it, allRecords),
                )
            }
        return mapOf(
            "rangeLabel" to range.label,
            "startAt" to range.start,
            "endExclusive" to range.endExclusive,
            "incomeCent" to totalIncome(this),
            "expenseCent" to totalExpense(this),
            "balanceCent" to totalIncome(this) - totalExpense(this),
            "categoryTotals" to filter { it.type == RecordType.INCOME || it.type == RecordType.EXPENSE }
                .groupBy { it.categoryId }
                .map { (categoryId, records) ->
                    mapOf(
                        "categoryId" to categoryId,
                        "categoryName" to categoryNameById[categoryId],
                        "type" to records.firstOrNull()?.type,
                        "amountCent" to records.sumOf { it.amountCent },
                    )
                },
            "accountSummary" to accountSummary,
        )
    }

    private fun ScheduledRecordEntity.toAgentMap(
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "type" to type,
        "amountCent" to amountCent,
        "categoryName" to categories.firstOrNull { it.id == categoryId }?.name,
        "fromAccountName" to accounts.firstOrNull { it.id == fromAccountId }?.name,
        "toAccountName" to accounts.firstOrNull { it.id == toAccountId }?.name,
        "frequency" to frequency,
        "nextRunAt" to nextRunAt,
        "note" to note.orEmpty(),
        "isEnabled" to isEnabled,
    )

    private fun formatDate(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))

    private data class ContextRange(
        val start: Long,
        val endExclusive: Long,
        val label: String,
    )

    private companion object {
        const val MAX_RECORD_CANDIDATES = 60
        const val MAX_SCHEDULES = 30
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L
        val accountOrRecordDirections = setOf(
            PresetSeedData.CATEGORY_INCOME,
            PresetSeedData.CATEGORY_EXPENSE,
            PresetSeedData.CATEGORY_ACCOUNT_ASSET,
            PresetSeedData.CATEGORY_ACCOUNT_LIABILITY,
        )
    }
}
