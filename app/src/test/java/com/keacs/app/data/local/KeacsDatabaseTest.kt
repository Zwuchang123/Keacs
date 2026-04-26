package com.keacs.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.keacs.app.data.local.database.KeacsDatabase
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.local.entity.RecordEntity
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.model.RecordType
import com.keacs.app.domain.rule.balanceFor
import com.keacs.app.domain.rule.totalExpense
import com.keacs.app.domain.rule.totalIncome
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KeacsDatabaseTest {
    private lateinit var database: KeacsDatabase
    private lateinit var repository: LocalDataRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeacsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LocalDataRepository(database) { 1_000L }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun databaseVersionIsCurrent() {
        assertEquals(2, database.openHelper.readableDatabase.version)
    }

    @Test
    fun initializePresetsCreatesCategoriesAndAccounts() = runTest {
        repository.initializePresets()

        val categories = repository.getCategories()
        val accounts = repository.getAccounts()

        assertEquals(19, categories.size)
        assertEquals(13, accounts.size)
        assertEquals(7, categories.count { it.direction == PresetSeedData.CATEGORY_INCOME })
        assertEquals(12, categories.count { it.direction == PresetSeedData.CATEGORY_EXPENSE })
        assertEquals(7, accounts.count { it.nature == PresetSeedData.ACCOUNT_ASSET })
        assertEquals(6, accounts.count { it.nature == PresetSeedData.ACCOUNT_LIABILITY })
        assertTrue(categories.all { it.isPreset && it.isEnabled })
        assertTrue(accounts.all { it.isEnabled && it.initialBalanceCent == 0L })
        assertEquals("1", repository.presetVersion())
    }

    @Test
    fun initializePresetsIsIdempotent() = runTest {
        repository.initializePresets()
        repository.initializePresets()

        assertEquals(19, database.categoryDao().count())
        assertEquals(13, database.accountDao().count())
    }

    @Test
    fun categoryAndAccountCanBeWrittenAndRead() = runTest {
        repository.initializePresets()

        val categoryNames = repository.getCategories().map { it.name }
        val accountNames = repository.getAccounts().map { it.name }

        assertTrue("工资" in categoryNames)
        assertTrue("餐饮" in categoryNames)
        assertTrue("支付宝" in accountNames)
        assertTrue("信用卡" in accountNames)
    }

    @Test
    fun categoryManagementValidatesAndSavesState() = runTest {
        repository.initializePresets()

        repository.saveCategory(
            id = null,
            name = "咖啡",
            direction = PresetSeedData.CATEGORY_EXPENSE,
            iconKey = "food",
            colorKey = "orange",
            isEnabled = true,
        )
        val created = repository.getCategories().first { it.name == "咖啡" }

        repository.saveCategory(
            id = created.id,
            name = "咖啡店",
            direction = PresetSeedData.CATEGORY_EXPENSE,
            iconKey = "food",
            colorKey = "orange",
            isEnabled = false,
        )

        val updated = repository.getCategories().first { it.id == created.id }
        assertEquals("咖啡店", updated.name)
        assertEquals(false, updated.isEnabled)
        assertEquals("已有同名分类，请换一个名称", runCatching {
            repository.saveCategory(null, "餐饮", PresetSeedData.CATEGORY_EXPENSE, "food", "orange", true)
        }.exceptionOrNull()?.message)
    }

    @Test
    fun accountManagementValidatesAndDeletesUnusedAccount() = runTest {
        repository.initializePresets()

        repository.saveAccount(
            id = null,
            name = "备用卡",
            nature = PresetSeedData.ACCOUNT_ASSET,
            type = "银行卡",
            iconKey = "bank",
            colorKey = "blue",
            initialBalanceCent = 12345,
            isEnabled = true,
        )
        val created = repository.getAccounts().first { it.name == "备用卡" }

        repository.deleteAccount(created.id)

        assertTrue(repository.getAccounts().none { it.id == created.id })
        assertEquals("已有同名账户，请换一个名称", runCatching {
            repository.saveAccount(null, "现金", PresetSeedData.ACCOUNT_ASSET, "现金", "wallet", "green", 0, true)
        }.exceptionOrNull()?.message)
    }

    @Test
    fun failedTransactionDoesNotKeepPartialData() = runTest {
        try {
            database.withTransaction {
                database.categoryDao().insert(
                    CategoryEntity(
                        name = "测试分类",
                        direction = PresetSeedData.CATEGORY_EXPENSE,
                        iconKey = "more",
                        colorKey = "gray",
                        isPreset = false,
                        isEnabled = true,
                        sortOrder = 99,
                        createdAt = 1_000L,
                        updatedAt = 1_000L,
                    ),
                )
                // 故意使用不存在的账户，验证事务失败时前面的写入会一起回滚。
                database.recordDao().insert(
                    RecordEntity(
                        type = "EXPENSE",
                        amountCent = 100,
                        occurredAt = 1_000L,
                        categoryId = null,
                        fromAccountId = 999,
                        toAccountId = null,
                        note = null,
                        createdAt = 1_000L,
                        updatedAt = 1_000L,
                    ),
                )
            }
        } catch (_: Exception) {
        }

        assertEquals(0, database.categoryDao().count())
        assertEquals(0, database.recordDao().count())
    }

    @Test
    fun recordRulesAffectBalancesAndBasicStats() = runTest {
        repository.initializePresets()
        val incomeCategory = repository.getCategories().first { it.name == "工资" }
        val expenseCategory = repository.getCategories().first { it.name == "餐饮" }
        val asset = repository.getAccounts().first { it.name == "现金" }
        val liability = repository.getAccounts().first { it.name == "信用卡" }

        repository.saveRecord(null, RecordType.INCOME, 10_000, 1_000, incomeCategory.id, null, asset.id, "")
        repository.saveRecord(null, RecordType.EXPENSE, 2_500, 1_000, expenseCategory.id, asset.id, null, "")
        repository.saveRecord(null, RecordType.EXPENSE, 5_000, 1_000, expenseCategory.id, liability.id, null, "")
        repository.saveRecord(null, RecordType.INCOME, 1_000, 1_000, incomeCategory.id, null, liability.id, "")
        repository.saveRecord(null, RecordType.TRANSFER, 2_000, 1_000, null, asset.id, liability.id, "")
        repository.saveRecord(null, RecordType.TRANSFER, 3_000, 1_000, null, liability.id, asset.id, "")

        val records = repository.getRecords()

        assertEquals(8_500, balanceFor(asset, records))
        assertEquals(3_000, balanceFor(liability, records))
        assertEquals(11_000, totalIncome(records))
        assertEquals(7_500, totalExpense(records))
    }

    @Test
    fun editingAndDeletingRecordRecalculatesBalance() = runTest {
        repository.initializePresets()
        val expenseCategory = repository.getCategories().first { it.name == "餐饮" }
        val asset = repository.getAccounts().first { it.name == "现金" }

        repository.saveRecord(null, RecordType.EXPENSE, 2_500, 1_000, expenseCategory.id, asset.id, null, "")
        val record = repository.getRecords().first()
        repository.saveRecord(record.id, RecordType.EXPENSE, 4_000, 1_000, expenseCategory.id, asset.id, null, "")

        assertEquals(-4_000, balanceFor(asset, repository.getRecords()))

        repository.deleteRecord(record.id)

        assertEquals(0, balanceFor(asset, repository.getRecords()))
        assertEquals(0, totalExpense(repository.getRecords()))
    }

    @Test
    fun recordValidationRejectsInvalidInput() = runTest {
        repository.initializePresets()
        val expenseCategory = repository.getCategories().first { it.name == "餐饮" }
        val asset = repository.getAccounts().first { it.name == "现金" }

        assertEquals("金额大于0才可保存", runCatching {
            repository.saveRecord(null, RecordType.EXPENSE, 0, 1_000, expenseCategory.id, asset.id, null, "")
        }.exceptionOrNull()?.message)
        assertEquals("转出和转入账户不能相同", runCatching {
            repository.saveRecord(null, RecordType.TRANSFER, 100, 1_000, null, asset.id, asset.id, "")
        }.exceptionOrNull()?.message)
    }
}
