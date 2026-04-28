package com.keacs.app.ui.record

import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.model.RecordType
import com.keacs.app.domain.usecase.CreateExpenseUseCase
import com.keacs.app.domain.usecase.CreateIncomeUseCase
import com.keacs.app.domain.usecase.CreateTransferUseCase
import com.keacs.app.domain.usecase.UpdateRecordUseCase
import java.math.BigDecimal
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
    if (key == "+" || key == "-") return nextOperatorAmount(current, key)

    val segment = current.substringAfterLastOperator()
    if (key == "." && segment.contains(".")) return current
    val nextSegment = when {
        key == "." && segment.isBlank() -> "0."
        segment == "0" && key != "." -> key
        else -> segment + key
    }
    if (nextSegment.substringAfter('.', "").length > 2) return current

    val prefix = current.dropLast(segment.length)
    val normalized = if (nextSegment.startsWith("0.")) nextSegment else nextSegment.trimStart('0').ifBlank { "0" }
    return prefix + normalized
}

fun amountToCent(text: String): Long? =
    runCatching {
        val value = evaluateAmountExpression(text) ?: return null
        if (value <= java.math.BigDecimal.ZERO || value.scale() > 2) return null
        value.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).toLong()
    }.getOrNull()

private fun nextOperatorAmount(current: String, key: String): String {
    if (current.isBlank()) return current
    return if (current.last().isAmountOperator()) {
        current.dropLast(1) + key
    } else {
        current + key
    }
}

private fun evaluateAmountExpression(text: String): BigDecimal? {
    if (text.isBlank() || text.last().isAmountOperator()) return null
    var total: BigDecimal? = null
    var operator = '+'
    var startIndex = 0
    text.forEachIndexed { index, char ->
        if (char.isAmountOperator()) {
            val value = text.substring(startIndex, index).toAmountPart() ?: return null
            total = applyAmountOperator(total, operator, value)
            operator = char
            startIndex = index + 1
        }
    }
    val lastValue = text.substring(startIndex).toAmountPart() ?: return null
    return applyAmountOperator(total, operator, lastValue)
}

private fun applyAmountOperator(total: BigDecimal?, operator: Char, value: BigDecimal): BigDecimal =
    when (operator) {
        '-' -> (total ?: BigDecimal.ZERO) - value
        else -> (total ?: BigDecimal.ZERO) + value
    }

private fun String.toAmountPart(): BigDecimal? {
    val value = toBigDecimalOrNull() ?: return null
    if (value.scale() > 2) return null
    return value
}

private fun String.substringAfterLastOperator(): String {
    val plusIndex = lastIndexOf('+')
    val minusIndex = lastIndexOf('-')
    val index = maxOf(plusIndex, minusIndex)
    return if (index >= 0) substring(index + 1) else this
}

private fun Char.isAmountOperator(): Boolean = this == '+' || this == '-'

fun centToInput(value: Long): String = DecimalFormat("0.00").format(value / 100.0)

fun dateLabel(time: Long): String = SimpleDateFormat("M月d日", Locale.CHINA).format(Date(time))

fun isSameDay(left: Long, right: Long): Boolean = dateLabel(left) == dateLabel(right)

const val ONE_DAY = 24 * 60 * 60 * 1000L
