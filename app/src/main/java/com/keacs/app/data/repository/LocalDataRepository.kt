package com.keacs.app.data.repository

import androidx.room.withTransaction
import com.keacs.app.data.local.database.KeacsDatabase
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.AppMetaEntity
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.local.entity.RecordEntity
import com.keacs.app.domain.model.RecordType
import kotlinx.coroutines.flow.Flow

class LocalDataRepository(
    private val database: KeacsDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    fun observeCategories(): Flow<List<CategoryEntity>> = database.categoryDao().observeAll()

    fun observeAccounts(): Flow<List<AccountEntity>> = database.accountDao().observeAll()

    fun observeRecords(): Flow<List<RecordEntity>> = database.recordDao().observeAll()

    suspend fun getCategories(): List<CategoryEntity> = database.categoryDao().getAll()

    suspend fun getAccounts(): List<AccountEntity> = database.accountDao().getAll()

    suspend fun getRecords(): List<RecordEntity> = database.recordDao().getAll()

    suspend fun getRecord(id: Long): RecordEntity? = database.recordDao().getById(id)

    suspend fun saveCategory(
        id: Long?,
        name: String,
        direction: String,
        iconKey: String,
        colorKey: String,
        isEnabled: Boolean,
    ) {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "名称不能为空" }
        require(database.categoryDao().countByName(trimmedName, direction, id ?: 0L) == 0) {
            "已有同名分类，请换一个名称"
        }
        val now = clock()
        if (id == null) {
            database.categoryDao().insert(
                CategoryEntity(
                    name = trimmedName,
                    direction = direction,
                    iconKey = iconKey,
                    colorKey = colorKey,
                    isPreset = false,
                    isEnabled = isEnabled,
                    sortOrder = (database.categoryDao().maxSortOrder(direction) ?: -1) + 1,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            val old = requireNotNull(database.categoryDao().getById(id)) { "分类不存在" }
            database.categoryDao().update(
                old.copy(
                    name = trimmedName,
                    direction = direction,
                    iconKey = iconKey,
                    colorKey = colorKey,
                    isEnabled = isEnabled,
                    updatedAt = now,
                ),
            )
        }
    }

    suspend fun deleteCategory(id: Long) {
        require(database.categoryDao().usageCount(id) == 0) { "已有历史记录，只能停用" }
        database.categoryDao().deleteById(id)
    }

    suspend fun saveAccount(
        id: Long?,
        name: String,
        nature: String,
        type: String,
        iconKey: String,
        colorKey: String,
        initialBalanceCent: Long,
        isEnabled: Boolean,
    ) {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "名称不能为空" }
        require(database.accountDao().countByName(trimmedName, id ?: 0L) == 0) {
            "已有同名账户，请换一个名称"
        }
        val now = clock()
        if (id == null) {
            database.accountDao().insert(
                AccountEntity(
                    name = trimmedName,
                    nature = nature,
                    type = type,
                    iconKey = iconKey,
                    colorKey = colorKey,
                    initialBalanceCent = initialBalanceCent,
                    isEnabled = isEnabled,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            val old = requireNotNull(database.accountDao().getById(id)) { "账户不存在" }
            database.accountDao().update(
                old.copy(
                    name = trimmedName,
                    nature = nature,
                    type = type,
                    iconKey = iconKey,
                    colorKey = colorKey,
                    initialBalanceCent = initialBalanceCent,
                    isEnabled = isEnabled,
                    updatedAt = now,
                ),
            )
        }
    }

    suspend fun deleteAccount(id: Long) {
        require(database.accountDao().usageCount(id) == 0) { "已有历史记录，只能停用" }
        database.accountDao().deleteById(id)
    }

    suspend fun saveRecord(
        id: Long?,
        type: String,
        amountCent: Long,
        occurredAt: Long,
        categoryId: Long?,
        fromAccountId: Long?,
        toAccountId: Long?,
        note: String?,
    ) {
        validateRecord(type, amountCent, categoryId, fromAccountId, toAccountId)
        val now = clock()
        if (id == null) {
            database.recordDao().insert(
                RecordEntity(
                    type = type,
                    amountCent = amountCent,
                    occurredAt = occurredAt,
                    categoryId = normalizedCategoryId(type, categoryId),
                    fromAccountId = normalizedFromAccountId(type, fromAccountId),
                    toAccountId = normalizedToAccountId(type, toAccountId),
                    note = note.orEmpty().trim().ifBlank { null },
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            val old = requireNotNull(database.recordDao().getById(id)) { "账目不存在" }
            database.recordDao().update(
                old.copy(
                    type = type,
                    amountCent = amountCent,
                    occurredAt = occurredAt,
                    categoryId = normalizedCategoryId(type, categoryId),
                    fromAccountId = normalizedFromAccountId(type, fromAccountId),
                    toAccountId = normalizedToAccountId(type, toAccountId),
                    note = note.orEmpty().trim().ifBlank { null },
                    updatedAt = now,
                ),
            )
        }
    }

    suspend fun deleteRecord(id: Long) {
        database.recordDao().deleteById(id)
    }

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

    private suspend fun validateRecord(
        type: String,
        amountCent: Long,
        categoryId: Long?,
        fromAccountId: Long?,
        toAccountId: Long?,
    ) {
        require(amountCent > 0) { "金额大于0才可保存" }
        when (type) {
            RecordType.INCOME -> {
                validateCategory(categoryId, PresetSeedData.CATEGORY_INCOME)
                toAccountId?.let { require(database.accountDao().getById(it) != null) { "账户不存在" } }
            }
            RecordType.EXPENSE -> {
                validateCategory(categoryId, PresetSeedData.CATEGORY_EXPENSE)
                fromAccountId?.let { require(database.accountDao().getById(it) != null) { "账户不存在" } }
            }
            RecordType.TRANSFER -> {
                require(fromAccountId != null) { "请选择转出账户" }
                require(toAccountId != null) { "请选择转入账户" }
                require(fromAccountId != toAccountId) { "转出和转入账户不能相同" }
                require(database.accountDao().getById(fromAccountId) != null) { "转出账户不存在" }
                require(database.accountDao().getById(toAccountId) != null) { "转入账户不存在" }
            }
            else -> error("账目类型不正确")
        }
    }

    private suspend fun validateCategory(categoryId: Long?, direction: String) {
        require(categoryId != null) { "请选择分类" }
        val category = requireNotNull(database.categoryDao().getById(categoryId)) { "分类不存在" }
        require(category.direction == direction) { "分类类型不正确" }
    }

    private fun normalizedCategoryId(type: String, categoryId: Long?): Long? =
        if (type == RecordType.TRANSFER) null else categoryId

    private fun normalizedFromAccountId(type: String, fromAccountId: Long?): Long? =
        when (type) {
            RecordType.EXPENSE, RecordType.TRANSFER -> fromAccountId
            else -> null
        }

    private fun normalizedToAccountId(type: String, toAccountId: Long?): Long? =
        when (type) {
            RecordType.INCOME -> toAccountId
            RecordType.TRANSFER -> toAccountId
            else -> null
        }
}
