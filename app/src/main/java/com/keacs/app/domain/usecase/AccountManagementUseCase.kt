package com.keacs.app.domain.usecase

import com.keacs.app.data.repository.LocalDataRepository

class AccountManagementUseCase(
    private val repository: LocalDataRepository,
) {
    suspend fun save(
        id: Long?,
        name: String,
        nature: String,
        type: String,
        iconKey: String,
        colorKey: String,
        initialBalanceCent: Long,
        isEnabled: Boolean,
    ) {
        repository.saveAccount(
            id = id,
            name = name,
            nature = nature,
            type = type,
            iconKey = iconKey,
            colorKey = colorKey,
            initialBalanceCent = initialBalanceCent,
            isEnabled = isEnabled,
        )
    }

    suspend fun delete(id: Long) {
        repository.deleteAccount(id)
    }
}
