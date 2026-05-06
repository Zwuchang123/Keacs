package com.keacs.app.domain.rule

import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.RecordEntity
import com.keacs.app.domain.model.RecordType

fun balanceFor(account: AccountEntity, records: List<RecordEntity>): Long {
    return account.initialBalanceCent
}

fun balanceAt(account: AccountEntity, records: List<RecordEntity>, endExclusive: Long): Long =
    account.initialBalanceCent - records
        .filter { it.occurredAt >= endExclusive }
        .sumOf { record -> accountEffect(account, record) }

fun totalIncome(records: List<RecordEntity>): Long =
    records.filter { it.type == RecordType.INCOME }.sumOf { it.amountCent }

fun totalExpense(records: List<RecordEntity>): Long =
    records.filter { it.type == RecordType.EXPENSE }.sumOf { it.amountCent }

fun accountEffect(account: AccountEntity, record: RecordEntity): Long {
    return when (record.type) {
        RecordType.INCOME -> incomeEffect(account, record)
        RecordType.EXPENSE -> expenseEffect(account, record)
        RecordType.TRANSFER -> transferEffect(account, record)
        else -> 0L
    }
}

private fun incomeEffect(account: AccountEntity, record: RecordEntity): Long {
    if (record.toAccountId != account.id) return 0L
    return if (account.nature == PresetSeedData.ACCOUNT_LIABILITY) -record.amountCent else record.amountCent
}

private fun expenseEffect(account: AccountEntity, record: RecordEntity): Long {
    if (record.fromAccountId != account.id) return 0L
    return if (account.nature == PresetSeedData.ACCOUNT_LIABILITY) record.amountCent else -record.amountCent
}

private fun transferEffect(account: AccountEntity, record: RecordEntity): Long {
    return when (account.id) {
        record.fromAccountId -> -record.amountCent
        record.toAccountId -> record.amountCent
        else -> 0L
    }
}
