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
    fun databaseVersionStartsAtOne() {
        assertEquals(1, database.openHelper.readableDatabase.version)
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
    fun failedTransactionDoesNotKeepPartialData() = runTest {
        try {
            database.withTransaction {
                database.categoryDao().insert(
                    CategoryEntity(
                        name = "测试分类",
                        direction = PresetSeedData.CATEGORY_EXPENSE,
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
}
