package com.keacs.app.data.agent

import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.data.repository.ScheduledFrequency
import com.keacs.app.data.repository.ScheduledRecordRepository
import com.keacs.app.domain.model.RecordType
import com.keacs.app.domain.usecase.CreateExpenseUseCase
import com.keacs.app.domain.usecase.CreateIncomeUseCase
import com.keacs.app.domain.usecase.CreateTransferUseCase
import com.keacs.app.domain.usecase.DeleteRecordUseCase
import com.keacs.app.domain.usecase.DisableScheduledRecordUseCase
import com.keacs.app.domain.usecase.SaveScheduledRecordUseCase
import com.keacs.app.domain.usecase.UpdateRecordUseCase

class AgentActionExecutor(
    private val repository: LocalDataRepository,
    private val scheduledRepository: ScheduledRecordRepository,
) {
    private val createIncome = CreateIncomeUseCase(repository)
    private val createExpense = CreateExpenseUseCase(repository)
    private val createTransfer = CreateTransferUseCase(repository)
    private val updateRecord = UpdateRecordUseCase(repository)
    private val deleteRecord = DeleteRecordUseCase(repository)
    private val saveScheduledRecord = SaveScheduledRecordUseCase(scheduledRepository)
    private val disableScheduledRecord = DisableScheduledRecordUseCase(scheduledRepository)

    suspend fun execute(action: AgentActionPreview): AgentExecutionResult =
        runCatching {
            val count = when (action.type) {
                "create_record" -> createRecords(action.records)
                "update_record" -> updateRecords(action.records)
                "delete_record" -> deleteRecords(action.records)
                "batch_update_records" -> updateRecords(action.records)
                "create_scheduled_record" -> saveSchedules(action.scheduledRecords, createOnly = true)
                "update_scheduled_record" -> saveSchedules(action.scheduledRecords, createOnly = false)
                "disable_scheduled_record" -> disableSchedules(action.scheduledRecords)
                else -> error("暂不支持该操作")
            }
            AgentExecutionResult.Success(count, successMessage(action.type, count))
        }.getOrElse { error ->
            AgentExecutionResult.Failure(error.message ?: "执行失败，请检查预览内容。")
        }

    private suspend fun createRecords(records: List<Map<String, Any?>>): Int {
        require(records.isNotEmpty()) { "没有可保存的账目" }
        val categories = repository.getCategories()
        val accounts = repository.getAccounts()
        records.forEach { item ->
            val type = item.stringValue("type") ?: RecordType.EXPENSE
            val amountCent = item.longValue("amountCent") ?: error("金额不正确")
            val occurredAt = item.longValue("occurredAt") ?: error("日期不正确")
            val note = item.stringValue("note").orEmpty()
            when (type) {
                RecordType.INCOME -> createIncome(
                    amountCent = amountCent,
                    occurredAt = occurredAt,
                    categoryId = item.resolveCategoryId(categories, PresetSeedData.CATEGORY_INCOME),
                    accountId = item.resolveAccountId(accounts, "toAccountId", "toAccountName", "accountName"),
                    note = note,
                )
                RecordType.TRANSFER -> createTransfer(
                    amountCent = amountCent,
                    occurredAt = occurredAt,
                    fromAccountId = item.resolveRequiredAccountId(accounts, "fromAccountId", "fromAccountName"),
                    toAccountId = item.resolveRequiredAccountId(accounts, "toAccountId", "toAccountName"),
                    note = note,
                )
                else -> createExpense(
                    amountCent = amountCent,
                    occurredAt = occurredAt,
                    categoryId = item.resolveCategoryId(categories, PresetSeedData.CATEGORY_EXPENSE),
                    accountId = item.resolveAccountId(accounts, "fromAccountId", "fromAccountName", "accountName"),
                    note = note,
                )
            }
        }
        return records.size
    }

    private suspend fun updateRecords(records: List<Map<String, Any?>>): Int {
        require(records.isNotEmpty()) { "没有可修改的账目" }
        val categories = repository.getCategories()
        val accounts = repository.getAccounts()
        records.forEach { item ->
            val id = item.longValue("id") ?: error("缺少账目")
            val old = requireNotNull(repository.getRecord(id)) { "账目不存在" }
            val type = item.stringValue("type") ?: old.type
            updateRecord(
                id = id,
                type = type,
                amountCent = item.longValue("amountCent") ?: old.amountCent,
                occurredAt = item.longValue("occurredAt") ?: old.occurredAt,
                categoryId = if (type == RecordType.TRANSFER) {
                    null
                } else {
                    item.resolveCategoryIdOrNull(categories, if (type == RecordType.INCOME) {
                        PresetSeedData.CATEGORY_INCOME
                    } else {
                        PresetSeedData.CATEGORY_EXPENSE
                    }) ?: old.categoryId
                },
                fromAccountId = when (type) {
                    RecordType.INCOME -> null
                    else -> item.resolveAccountId(accounts, "fromAccountId", "fromAccountName", "accountName")
                        ?: old.fromAccountId
                },
                toAccountId = when (type) {
                    RecordType.EXPENSE -> null
                    else -> item.resolveAccountId(accounts, "toAccountId", "toAccountName", "accountName")
                        ?: old.toAccountId
                },
                note = item.stringValue("note") ?: old.note.orEmpty(),
            )
        }
        return records.size
    }

    private suspend fun deleteRecords(records: List<Map<String, Any?>>): Int {
        require(records.isNotEmpty()) { "没有可删除的账目" }
        records.forEach { item ->
            deleteRecord(item.longValue("id") ?: error("缺少账目"))
        }
        return records.size
    }

    private suspend fun saveSchedules(
        schedules: List<Map<String, Any?>>,
        createOnly: Boolean,
    ): Int {
        require(schedules.isNotEmpty()) { "没有可保存的定时记账" }
        val categories = repository.getCategories()
        val accounts = repository.getAccounts()
        schedules.forEach { item ->
            val id = item.longValue("id")
            require(!createOnly || id == null) { "新增定时记账不能带已有编号" }
            val old = id?.let { scheduledRepository.getSchedule(it) }
            val type = item.stringValue("type") ?: old?.type ?: RecordType.EXPENSE
            saveScheduledRecord(
                id = id,
                type = type,
                amountCent = item.longValue("amountCent") ?: old?.amountCent ?: error("金额不正确"),
                categoryId = if (type == RecordType.TRANSFER) {
                    null
                } else {
                    item.resolveCategoryIdOrNull(categories, if (type == RecordType.INCOME) {
                        PresetSeedData.CATEGORY_INCOME
                    } else {
                        PresetSeedData.CATEGORY_EXPENSE
                    }) ?: old?.categoryId
                },
                fromAccountId = when (type) {
                    RecordType.INCOME -> null
                    else -> item.resolveAccountId(accounts, "fromAccountId", "fromAccountName", "accountName")
                        ?: old?.fromAccountId
                },
                toAccountId = when (type) {
                    RecordType.EXPENSE -> null
                    else -> item.resolveAccountId(accounts, "toAccountId", "toAccountName", "accountName")
                        ?: old?.toAccountId
                },
                frequency = item.stringValue("frequency") ?: old?.frequency ?: ScheduledFrequency.MONTHLY,
                recurrenceValues = item.stringValue("recurrenceValues") ?: old?.recurrenceValues,
                nextRunAt = item.longValue("nextRunAt") ?: old?.nextRunAt ?: error("下次生成时间不正确"),
                note = item.stringValue("note") ?: old?.note.orEmpty(),
                isEnabled = item.booleanValue("isEnabled") ?: old?.isEnabled ?: true,
            )
        }
        return schedules.size
    }

    private suspend fun disableSchedules(schedules: List<Map<String, Any?>>): Int {
        require(schedules.isNotEmpty()) { "没有可停用的定时记账" }
        schedules.forEach { item ->
            disableScheduledRecord(item.longValue("id") ?: error("缺少定时记账"))
        }
        return schedules.size
    }

    private suspend fun Map<String, Any?>.resolveCategoryId(
        categories: List<com.keacs.app.data.local.entity.CategoryEntity>,
        direction: String,
    ): Long = resolveCategoryIdOrNull(categories, direction) ?: error("分类不存在")

    private fun Map<String, Any?>.resolveCategoryIdOrNull(
        categories: List<com.keacs.app.data.local.entity.CategoryEntity>,
        direction: String,
    ): Long? {
        longValue("categoryId")?.let { return it }
        val name = stringValue("categoryName") ?: stringValue("category")
        return categories.firstOrNull { it.direction == direction && it.name == name }?.id
    }

    private fun Map<String, Any?>.resolveRequiredAccountId(
        accounts: List<com.keacs.app.data.local.entity.AccountEntity>,
        idKey: String,
        nameKey: String,
    ): Long = resolveAccountId(accounts, idKey, nameKey) ?: error("账户不存在")

    private fun Map<String, Any?>.resolveAccountId(
        accounts: List<com.keacs.app.data.local.entity.AccountEntity>,
        idKey: String,
        nameKey: String,
        fallbackNameKey: String? = null,
    ): Long? {
        longValue(idKey)?.let { return it }
        val name = stringValue(nameKey) ?: fallbackNameKey?.let { stringValue(it) }
        return accounts.firstOrNull { it.name == name }?.id
    }

    private fun Map<String, Any?>.stringValue(key: String): String? =
        (this[key] as? String)?.trim()?.takeIf { it.isNotBlank() }

    private fun Map<String, Any?>.longValue(key: String): Long? =
        when (val value = this[key]) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }

    private fun Map<String, Any?>.booleanValue(key: String): Boolean? =
        when (val value = this[key]) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull()
            else -> null
        }

    private fun successMessage(type: String, count: Int): String = when (type) {
        "create_record" -> "已新增 $count 笔账目"
        "update_record", "batch_update_records" -> "已修改 $count 笔账目"
        "delete_record" -> "已删除 $count 笔账目"
        "create_scheduled_record" -> "已新增 $count 条定时记账"
        "update_scheduled_record" -> "已修改 $count 条定时记账"
        "disable_scheduled_record" -> "已停用 $count 条定时记账"
        else -> "操作已完成"
    }
}

sealed interface AgentExecutionResult {
    data class Success(val impactCount: Int, val message: String) : AgentExecutionResult
    data class Failure(val message: String) : AgentExecutionResult
}
