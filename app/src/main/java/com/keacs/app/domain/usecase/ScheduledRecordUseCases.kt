package com.keacs.app.domain.usecase

import com.keacs.app.data.repository.ScheduledRecordRepository

class GenerateDueScheduledRecordsUseCase(
    private val repository: ScheduledRecordRepository,
) {
    suspend operator fun invoke(): Int = repository.createDueRecords()
}
