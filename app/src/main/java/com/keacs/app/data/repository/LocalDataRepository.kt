package com.keacs.app.data.repository

import androidx.room.withTransaction
import com.keacs.app.data.local.database.KeacsDatabase
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.AppMetaEntity
import com.keacs.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

class LocalDataRepository(
    private val database: KeacsDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    fun observeCategories(): Flow<List<CategoryEntity>> = database.categoryDao().observeAll()

    fun observeAccounts(): Flow<List<AccountEntity>> = database.accountDao().observeAll()

    suspend fun getCategories(): List<CategoryEntity> = database.categoryDao().getAll()

    suspend fun getAccounts(): List<AccountEntity> = database.accountDao().getAll()

    suspend fun initializePresets() {
        database.withTransaction {
            val now = clock()
            if (database.categoryDao().count() == 0) {
                database.categoryDao().insertAll(PresetSeedData.categories(now))
            }
            if (database.accountDao().count() == 0) {
                database.accountDao().insertAll(PresetSeedData.accounts(now))
            }
            // 记录初始化版本，后续迁移和排查可直接判断本地预置数据状态。
            database.appMetaDao().upsert(
                AppMetaEntity(
                    key = META_PRESET_VERSION,
                    value = "1",
                    updatedAt = now,
                ),
            )
        }
    }

    suspend fun presetVersion(): String? {
        return database.appMetaDao().get(META_PRESET_VERSION)?.value
    }

    private companion object {
        const val META_PRESET_VERSION = "preset_version"
    }
}
