package com.keacs.app.domain.usecase

import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.model.RecordType

class CreateIncomeUseCase(private val repository: LocalDataRepository) {
    suspend operator fun invoke(amountCent: Long, occurredAt: Long, categoryId: Long, accountId: Long?, note: String) {
        repository.saveRecord(null, RecordType.INCOME, amountCent, occurredAt, categoryId, null, accountId, note)
    }
}

class CreateExpenseUseCase(private val repository: LocalDataRepository) {
    suspend operator fun invoke(amountCent: Long, occurredAt: Long, categoryId: Long, accountId: Long?, note: String) {
        repository.saveRecord(null, RecordType.EXPENSE, amountCent, occurredAt, categoryId, accountId, null, note)
    }
}

class CreateTransferUseCase(private val repository: LocalDataRepository) {
    suspend operator fun invoke(amountCent: Long, occurredAt: Long, fromAccountId: Long, toAccountId: Long, note: String) {
        repository.saveRecord(null, RecordType.TRANSFER, amountCent, occurredAt, null, fromAccountId, toAccountId, note)
    }
}

class UpdateRecordUseCase(private val repository: LocalDataRepository) {
    suspend operator fun invoke(
        id: Long,
        type: String,
        amountCent: Long,
        occurredAt: Long,
        categoryId: Long?,
        fromAccountId: Long?,
        toAccountId: Long?,
        note: String,
    ) {
        repository.saveRecord(id, type, amountCent, occurredAt, categoryId, fromAccountId, toAccountId, note)
    }
}

class DeleteRecordUseCase(private val repository: LocalDataRepository) {
    suspend operator fun invoke(id: Long) {
        repository.deleteRecord(id)
    }
}
