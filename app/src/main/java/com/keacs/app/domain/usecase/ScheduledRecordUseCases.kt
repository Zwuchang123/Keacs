package com.keacs.app.domain.usecase

import com.keacs.app.data.repository.ScheduledRecordRepository

class GenerateDueScheduledRecordsUseCase(
    private val repository: ScheduledRecordRepository,
) {
    suspend operator fun invoke(): Int = repository.createDueRecords()
}

class SaveScheduledRecordUseCase(
    private val repository: ScheduledRecordRepository,
) {
    suspend operator fun invoke(
        id: Long?,
        type: String,
        amountCent: Long,
        categoryId: Long?,
        fromAccountId: Long?,
        toAccountId: Long?,
        frequency: String,
        recurrenceValues: String?,
        nextRunAt: Long,
        note: String,
        isEnabled: Boolean,
    ) {
        repository.saveSchedule(
            id = id,
            type = type,
            amountCent = amountCent,
            categoryId = categoryId,
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            frequency = frequency,
            recurrenceValues = recurrenceValues,
            nextRunAt = nextRunAt,
            note = note,
            isEnabled = isEnabled,
        )
    }
}

class DisableScheduledRecordUseCase(
    private val repository: ScheduledRecordRepository,
) {
    suspend operator fun invoke(id: Long) {
        val old = requireNotNull(repository.getSchedule(id)) { "定时记账不存在" }
        repository.saveSchedule(
            id = old.id,
            type = old.type,
            amountCent = old.amountCent,
            categoryId = old.categoryId,
            fromAccountId = old.fromAccountId,
            toAccountId = old.toAccountId,
            frequency = old.frequency,
            recurrenceValues = old.recurrenceValues,
            nextRunAt = old.nextRunAt,
            note = old.note.orEmpty(),
            isEnabled = false,
        )
    }
}
