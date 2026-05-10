package com.keacs.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.viewModelScope
import com.keacs.app.data.local.database.KeacsDatabase
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.local.entity.RecordEntity
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.model.RecordType
import com.keacs.app.domain.rule.balanceFor
import com.keacs.app.domain.rule.totalExpense
import com.keacs.app.domain.rule.totalIncome
import com.keacs.app.ui.stats.StatsTab
import com.keacs.app.ui.stats.StatsViewModel
import com.keacs.app.ui.stats.TimePeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.Locale
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
        assertEquals(5, database.openHelper.readableDatabase.version)
    }

    @Test
    fun initializePresetsCreatesCategoriesAndAccounts() = runTest {
        repository.initializePresets()

        val categories = repository.getCategories()
        val accounts = repository.getAccounts()

        assertEquals(37, categories.size)
        assertEquals(13, accounts.size)
        assertEquals(7, categories.count { it.direction == PresetSeedData.CATEGORY_INCOME })
        assertEquals(17, categories.count { it.direction == PresetSeedData.CATEGORY_EXPENSE })
        assertEquals(7, categories.count { it.direction == PresetSeedData.CATEGORY_ACCOUNT_ASSET })
        assertEquals(6, categories.count { it.direction == PresetSeedData.CATEGORY_ACCOUNT_LIABILITY })
        assertEquals(7, accounts.count { it.nature == PresetSeedData.ACCOUNT_ASSET })
        assertEquals(6, accounts.count { it.nature == PresetSeedData.ACCOUNT_LIABILITY })
        assertTrue(categories.all { it.isPreset && it.isEnabled })
        assertTrue(accounts.all { it.isEnabled && it.initialBalanceCent == 0L })
        assertEquals("5", repository.presetVersion())
    }

    @Test
    fun initializePresetsIsIdempotent() = runTest {
        repository.initializePresets()
        repository.initializePresets()

        assertEquals(37, database.categoryDao().count())
        assertEquals(13, database.accountDao().count())
    }

    @Test
    fun legacyAccountCategoriesAreSplitByNature() = runTest {
        val now = 1_000L
        database.categoryDao().insert(
            CategoryEntity(
                name = "现金",
                direction = PresetSeedData.CATEGORY_ACCOUNT,
                iconKey = "wallet",
                colorKey = "green",
                isPreset = true,
                isEnabled = true,
                sortOrder = 0,
                createdAt = now,
                updatedAt = now,
            ),
        )
        database.categoryDao().insert(
            CategoryEntity(
                name = "信用卡",
                direction = PresetSeedData.CATEGORY_ACCOUNT,
                iconKey = "card",
                colorKey = "orange",
                isPreset = true,
                isEnabled = true,
                sortOrder = 1,
                createdAt = now,
                updatedAt = now,
            ),
        )

        repository.initializePresets()

        val categories = repository.getCategories()
        assertTrue(categories.any { it.name == "现金" && it.direction == PresetSeedData.CATEGORY_ACCOUNT_ASSET })
        assertTrue(categories.any { it.name == "信用卡" && it.direction == PresetSeedData.CATEGORY_ACCOUNT_LIABILITY })
        assertFalse(categories.any { it.direction == PresetSeedData.CATEGORY_ACCOUNT })
    }

    @Test
    fun deletedPresetAccountDoesNotComeBackAfterInitialize() = runTest {
        repository.initializePresets()
        val cash = repository.getAccounts().first { it.name == "现金" }

        repository.deleteAccount(cash.id)
        repository.initializePresets()

        assertTrue(repository.getAccounts().none { it.name == "现金" })
        assertEquals(12, database.accountDao().count())
    }

    @Test
    fun categoryAndAccountCanBeWrittenAndRead() = runTest {
        repository.initializePresets()

        val categoryNames = repository.getCategories().map { it.name }
        val accountNames = repository.getAccounts().map { it.name }

        assertTrue("工资" in categoryNames)
        assertTrue("餐饮" in categoryNames)
        assertTrue("支付宝" in categoryNames)
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
        assertEquals("名称不能超过4个字", runCatching {
            repository.saveCategory(null, "超长分类名", PresetSeedData.CATEGORY_EXPENSE, "food", "orange", true)
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
    fun liabilityAccountStoresSignedBalance() = runTest {
        repository.initializePresets()

        repository.saveAccount(
            id = null,
            name = "备用负债",
            nature = PresetSeedData.ACCOUNT_LIABILITY,
            type = "信用卡",
            iconKey = "card",
            colorKey = "orange",
            initialBalanceCent = 12_345,
            isEnabled = true,
        )

        val created = repository.getAccounts().first { it.name == "备用负债" }
        assertEquals(-12_345, created.initialBalanceCent)
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
        val updatedAsset = repository.getAccounts().first { it.id == asset.id }
        val updatedLiability = repository.getAccounts().first { it.id == liability.id }

        assertEquals(8_500, balanceFor(updatedAsset, records))
        assertEquals(-5_000, balanceFor(updatedLiability, records))
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

        val updatedAsset = repository.getAccounts().first { it.id == asset.id }
        assertEquals(-4_000, balanceFor(updatedAsset, repository.getRecords()))

        repository.deleteRecord(record.id)

        val restoredAsset = repository.getAccounts().first { it.id == asset.id }
        assertEquals(0, balanceFor(restoredAsset, repository.getRecords()))
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun statsUseSelectedMonthAndYearBuckets() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        repository.initializePresets()
        val incomeCategory = repository.getCategories().first { it.name == "工资" }
        val expenseCategory = repository.getCategories().first { it.name == "餐饮" }
        val asset = repository.getAccounts().first { it.name == "现金" }
        val liability = repository.getAccounts().first { it.name == "信用卡" }
        val jan3 = dateMillis(2026, 1, 3)
        val jan5 = dateMillis(2026, 1, 5)
        val feb2 = dateMillis(2026, 2, 2)

        repository.saveRecord(null, RecordType.EXPENSE, 12_000, jan3, expenseCategory.id, asset.id, null, "")
        repository.saveRecord(null, RecordType.EXPENSE, 8_000, jan5, expenseCategory.id, liability.id, null, "")
        repository.saveRecord(null, RecordType.INCOME, 50_000, feb2, incomeCategory.id, null, asset.id, "")

        val viewModel = StatsViewModel(repository) { dateMillis(2026, 5, 6) + 12 * 60 * 60 * 1000L }
        try {
            var state = viewModel.uiState.first { !it.isLoading }
            assertEquals(TimePeriod.YEAR, state.selectedPeriod)

            viewModel.selectPeriod(TimePeriod.YEAR)
            viewModel.selectDate(dateMillis(2026, 1, 1))
            state = viewModel.uiState.first {
                !it.isLoading && it.selectedPeriod == TimePeriod.YEAR
            }
            assertEquals(50_000, state.income)
            assertEquals(20_000, state.expense)
            assertEquals(20_000, state.dailyTrend.first { it.day == 1 }.amount)
            assertEquals(30_000, state.netAsset)
            assertTrue(state.dailyTrend.none { it.day > 5 })

            viewModel.selectTab(StatsTab.INCOME)
            state = viewModel.uiState.first { !it.isLoading && it.selectedTab == StatsTab.INCOME }
            assertEquals(50_000, state.dailyTrend.first { it.day == 2 }.amount)

            viewModel.selectTab(StatsTab.EXPENSE)
            viewModel.selectPeriod(TimePeriod.MONTH)
            viewModel.selectDate(jan3)
            state = viewModel.uiState.first {
                !it.isLoading && it.selectedPeriod == TimePeriod.MONTH && it.selectedTab == StatsTab.EXPENSE
            }
            assertEquals(12_000, state.dailyTrend.first { it.day == 3 }.amount)
            assertEquals(8_000, state.dailyTrend.first { it.day == 5 }.amount)

            viewModel.selectTab(StatsTab.ASSET)
            state = viewModel.uiState.first { !it.isLoading && it.selectedTab == StatsTab.ASSET }
            assertEquals(-20_000, state.netAsset)
            assertEquals(-12_000, state.balanceTrend.first { it.day == 3 }.amount)
            assertEquals(0, state.balanceTrend.first { it.day == 4 }.amount)
            assertEquals(-8_000, state.balanceTrend.first { it.day == 5 }.amount)
            assertEquals(0, state.balanceTrend.last().amount)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun importBackupMergesDuplicateCategoriesAndRenamesDuplicateAccounts() = runTest {
        repository.initializePresets()
        val originalCategoryCount = repository.getCategories().size
        val duplicateCategory = CategoryEntity(
            id = 900,
            name = "餐饮",
            direction = PresetSeedData.CATEGORY_EXPENSE,
            iconKey = "food",
            colorKey = "orange",
            isPreset = false,
            isEnabled = true,
            sortOrder = 0,
            createdAt = 1_000L,
            updatedAt = 1_000L,
        )
        val duplicateAccount = com.keacs.app.data.local.entity.AccountEntity(
            id = 900,
            name = "现金",
            nature = PresetSeedData.ACCOUNT_ASSET,
            type = "现金",
            iconKey = "wallet",
            colorKey = "green",
            initialBalanceCent = 0,
            isEnabled = true,
            createdAt = 1_000L,
            updatedAt = 1_000L,
        )
        val duplicateRecord = RecordEntity(
            type = RecordType.EXPENSE,
            amountCent = 100,
            occurredAt = 1_000L,
            categoryId = duplicateCategory.id,
            fromAccountId = null,
            toAccountId = null,
            note = null,
            createdAt = 1_000L,
            updatedAt = 1_000L,
        )

        repository.importBackup(
            categories = listOf(duplicateCategory),
            accounts = listOf(duplicateAccount),
            records = listOf(duplicateRecord),
        )

        val categories = repository.getCategories().filter { it.direction == PresetSeedData.CATEGORY_EXPENSE }
        val accounts = repository.getAccounts()
        val records = repository.getRecords()
        val existingCategory = categories.first { it.name == "餐饮" }

        assertEquals(originalCategoryCount, repository.getCategories().size)
        assertTrue(categories.any { it.name == "餐饮" })
        assertFalse(categories.any { it.name.contains("导入") })
        assertEquals(existingCategory.id, records.single().categoryId)
        assertTrue(accounts.any { it.name == "现金" })
        assertTrue(accounts.any { it.name == "现金（导入2）" })
    }

    @Test
    fun importOldBackupConvertsLiabilityBalanceToSignedValue() = runTest {
        repository.initializePresets()
        val oldLiability = com.keacs.app.data.local.entity.AccountEntity(
            id = 901,
            name = "旧信用卡",
            nature = PresetSeedData.ACCOUNT_LIABILITY,
            type = "信用卡",
            iconKey = "card",
            colorKey = "orange",
            initialBalanceCent = 5_000,
            isEnabled = true,
            createdAt = 1_000L,
            updatedAt = 1_000L,
        )

        repository.importBackup(
            categories = emptyList(),
            accounts = listOf(oldLiability),
            records = emptyList(),
            backupVersion = 1,
        )

        assertEquals(-5_000, repository.getAccounts().first { it.name == "旧信用卡" }.initialBalanceCent)
    }

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(Locale.getDefault()).apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
