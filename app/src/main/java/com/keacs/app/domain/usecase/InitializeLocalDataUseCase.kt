package com.keacs.app.domain.usecase

import com.keacs.app.data.repository.LocalDataRepository

class InitializeLocalDataUseCase(
    private val repository: LocalDataRepository,
) {
    suspend operator fun invoke() {
        repository.initializePresets()
    }
}
