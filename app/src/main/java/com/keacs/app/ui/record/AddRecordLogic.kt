package com.keacs.app.ui.record

import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.model.RecordType
import com.keacs.app.domain.usecase.CreateExpenseUseCase
import com.keacs.app.domain.usecase.CreateIncomeUseCase
import com.keacs.app.domain.usecase.CreateTransferUseCase
import com.keacs.app.domain.usecase.UpdateRecordUseCase
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

suspend fun saveRecord(
    repository: LocalDataRepository,
    recordId: Long?,
    type: String,
    amountCent: Long,
    occurredAt: Long,
    categoryId: Long?,
    accountId: Long?,
    fromAccountId: Long?,
    toAccountId: Long?,
    note: String,
) {
    if (recordId != null) {
        val fromId = if (type == RecordType.EXPENSE) accountId else fromAccountId
        val toId = if (type == RecordType.INCOME) accountId else toAccountId
        UpdateRecordUseCase(repository)(recordId, type, amountCent, occurredAt, categoryId, fromId, toId, note)
    } else {
        when (type) {
            RecordType.INCOME -> CreateIncomeUseCase(repository)(amountCent, occurredAt, requireNotNull(categoryId), accountId, note)
            RecordType.EXPENSE -> CreateExpenseUseCase(repository)(amountCent, occurredAt, requireNotNull(categoryId), accountId, note)
            else -> CreateTransferUseCase(repository)(amountCent, occurredAt, requireNotNull(fromAccountId), requireNotNull(toAccountId), note)
        }
    }
}

fun validationText(type: String, amount: Long?, categoryId: Long?, fromId: Long?, toId: Long?, error: String?): String? =
    error ?: when {
        amount == null -> null
        type != RecordType.TRANSFER && categoryId == null -> "请选择分类"
        type == RecordType.TRANSFER && fromId == toId -> "转出和转入账户不能相同"
        type == RecordType.TRANSFER && (fromId == null || toId == null) -> "请选择转账账户"
        else -> null
    }

fun typeIndex(type: String): Int = when (type) {
    RecordType.INCOME -> 1
    RecordType.TRANSFER -> 2
    else -> 0
}

fun nextAmount(current: String, key: String): String {
    if (key == "⌫") return current.dropLast(1)
    if (key == "+" || key == "-") return current
    if (key == "." && current.contains(".")) return current
    val next = if (key == "." && current.isBlank()) "0." else current + key
    if (next.substringAfter('.', "").length > 2) return current
    return if (next.startsWith("0.")) next else next.trimStart('0').ifBlank { "0" }
}

fun amountToCent(text: String): Long? =
    runCatching {
        val value = text.toBigDecimalOrNull() ?: return null
        if (value <= java.math.BigDecimal.ZERO || value.scale() > 2) return null
        value.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).toLong()
    }.getOrNull()

fun centToInput(value: Long): String = DecimalFormat("0.00").format(value / 100.0)

fun dateLabel(time: Long): String = SimpleDateFormat("M月d日", Locale.CHINA).format(Date(time))

fun isSameDay(left: Long, right: Long): Boolean = dateLabel(left) == dateLabel(right)

const val ONE_DAY = 24 * 60 * 60 * 1000L
