package com.keacs.app.domain.usecase

import com.keacs.app.data.repository.LocalDataRepository

class CategoryManagementUseCase(
    private val repository: LocalDataRepository,
) {
    suspend fun save(
        id: Long?,
        name: String,
        direction: String,
        iconKey: String,
        colorKey: String,
        isEnabled: Boolean,
    ) {
        repository.saveCategory(id, name, direction, iconKey, colorKey, isEnabled)
    }

    suspend fun delete(id: Long) {
        repository.deleteCategory(id)
    }
}
