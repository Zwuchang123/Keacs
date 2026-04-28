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
        require(trimmedName.length <= MAX_CATEGORY_NAME_LENGTH) { "名称不能超过4个字" }
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
            // 预置项需要支持后续补齐，避免老用户升级后缺少新增的预置分类。
            database.categoryDao().insertAll(PresetSeedData.categories(now))
            database.accountDao().insertAll(PresetSeedData.accounts(now))
            // 记录初始化版本，后续迁移和排查可直接判断本地预置数据状态。
            database.appMetaDao().upsert(
                AppMetaEntity(
                    key = META_PRESET_VERSION,
                    value = PRESET_VERSION,
                    updatedAt = now,
                ),
            )
        }
    }

    suspend fun presetVersion(): String? = database.appMetaDao().get(META_PRESET_VERSION)?.value

    suspend fun importBackup(categories: List<CategoryEntity>, accounts: List<AccountEntity>, records: List<RecordEntity>) {
        database.withTransaction {
            val now = clock()
            val categoryIdMap = mutableMapOf<Long, Long>()
            val categoryByName = database.categoryDao().getAll()
                .associateBy { it.direction to it.name }
                .toMutableMap()

            for (cat in categories) {
                val key = cat.direction to cat.name
                val existing = categoryByName[key]
                if (existing != null) {
                    categoryIdMap[cat.id] = existing.id
                    continue
                }
                val newId = database.categoryDao().insert(
                    cat.copy(
                        id = 0,
                        createdAt = now,
                        updatedAt = now,
                        sortOrder = (database.categoryDao().maxSortOrder(cat.direction) ?: -1) + 1
                    )
                )
                categoryIdMap[cat.id] = newId
                categoryByName[key] = cat.copy(id = newId)
            }

            val accountIdMap = mutableMapOf<Long, Long>()
            val usedAccountNames = database.accountDao().getAll()
                .map { it.name }
                .toMutableSet()

            for (acc in accounts) {
                val resolvedName = uniqueAccountName(acc.name, usedAccountNames)
                val newId = database.accountDao().insert(
                    acc.copy(
                        id = 0,
                        name = resolvedName,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
                accountIdMap[acc.id] = newId
            }

            for (rec in records) {
                val newCategoryId = rec.categoryId?.let { categoryIdMap[it] }
                val newFromAccountId = rec.fromAccountId?.let { accountIdMap[it] }
                val newToAccountId = rec.toAccountId?.let { accountIdMap[it] }

                database.recordDao().insert(
                    rec.copy(
                        id = 0,
                        categoryId = newCategoryId,
                        fromAccountId = newFromAccountId,
                        toAccountId = newToAccountId,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }
    }

    private companion object {
        const val META_PRESET_VERSION = "preset_version"
        const val PRESET_VERSION = "2"
        const val MAX_CATEGORY_NAME_LENGTH = 4
    }

    private fun uniqueAccountName(baseName: String, usedNames: MutableSet<String>): String {
        var candidate = baseName
        var suffix = 2
        while (!usedNames.add(candidate)) {
            candidate = "$baseName（导入$suffix）"
            suffix += 1
        }
        return candidate
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
