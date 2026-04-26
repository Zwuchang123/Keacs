package com.keacs.app.data.backup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keacs.app.data.local.database.KeacsDatabase
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.local.entity.RecordEntity
import com.keacs.app.data.repository.LocalDataRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class BackupServiceTest {

    private lateinit var database: KeacsDatabase
    private lateinit var repository: LocalDataRepository
    private lateinit var backupService: BackupService

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KeacsDatabase::class.java
        ).build()

        repository = LocalDataRepository(database) { System.currentTimeMillis() }
        backupService = BackupService(repository)

        runBlocking {
            repository.initializePresets()
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `export backup contains categories accounts records`() = runBlocking {
        val outputStream = ByteArrayOutputStream()
        backupService.exportBackup(outputStream)

        val json = JSONObject(outputStream.toString())
        assertEquals(1, json.getInt("backupVersion"))
        assertTrue(json.has("exportedAt"))
        assertTrue(json.has("categories"))
        assertTrue(json.has("accounts"))
        assertTrue(json.has("records"))

        val categories = json.getJSONArray("categories")
        assertTrue(categories.length() > 0)

        val accounts = json.getJSONArray("accounts")
        assertTrue(accounts.length() > 0)
    }

    @Test
    fun `import backup merges data into existing`() = runBlocking {
        val originalCategories = repository.getCategories()
        val originalAccounts = repository.getAccounts()

        val testCategory = CategoryEntity(
            id = 0,
            name = "测试分类",
            direction = "expense",
            iconKey = "test",
            colorKey = "blue",
            isPreset = false,
            isEnabled = true,
            sortOrder = 100,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val testAccount = AccountEntity(
            id = 0,
            name = "测试账户",
            nature = "asset",
            type = "cash",
            iconKey = "wallet",
            colorKey = "green",
            initialBalanceCent = 10000L,
            isEnabled = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val testRecord = RecordEntity(
            id = 0,
            type = "expense",
            amountCent = 5000L,
            occurredAt = System.currentTimeMillis(),
            categoryId = null,
            fromAccountId = null,
            toAccountId = null,
            note = "测试记录",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val backupJson = JSONObject().apply {
            put("backupVersion", 1)
            put("exportedAt", System.currentTimeMillis())
            put("categories", org.json.JSONArray().put(JSONObject().apply {
                put("id", 999L)
                put("name", testCategory.name)
                put("direction", testCategory.direction)
                put("iconKey", testCategory.iconKey)
                put("colorKey", testCategory.colorKey)
                put("isPreset", testCategory.isPreset)
                put("isEnabled", testCategory.isEnabled)
                put("sortOrder", testCategory.sortOrder)
                put("createdAt", testCategory.createdAt)
                put("updatedAt", testCategory.updatedAt)
            }))
            put("accounts", org.json.JSONArray().put(JSONObject().apply {
                put("id", 999L)
                put("name", testAccount.name)
                put("nature", testAccount.nature)
                put("type", testAccount.type)
                put("iconKey", testAccount.iconKey)
                put("colorKey", testAccount.colorKey)
                put("initialBalanceCent", testAccount.initialBalanceCent)
                put("isEnabled", testAccount.isEnabled)
                put("createdAt", testAccount.createdAt)
                put("updatedAt", testAccount.updatedAt)
            }))
            put("records", org.json.JSONArray().put(JSONObject().apply {
                put("id", 999L)
                put("type", testRecord.type)
                put("amountCent", testRecord.amountCent)
                put("occurredAt", testRecord.occurredAt)
                put("categoryId", testRecord.categoryId)
                put("fromAccountId", testRecord.fromAccountId)
                put("toAccountId", testRecord.toAccountId)
                put("note", testRecord.note)
                put("createdAt", testRecord.createdAt)
                put("updatedAt", testRecord.updatedAt)
            }))
        }

        val inputStream = ByteArrayInputStream(backupJson.toString().toByteArray())
        backupService.importBackup(inputStream)

        val afterCategories = repository.getCategories()
        val afterAccounts = repository.getAccounts()
        val afterRecords = repository.getRecords()

        assertTrue(afterCategories.size > originalCategories.size)
        assertTrue(afterAccounts.size > originalAccounts.size)
        assertTrue(afterRecords.isNotEmpty())
    }

    @Test
    fun `import backup rejects invalid version`() = runBlocking {
        val invalidJson = JSONObject().apply {
            put("backupVersion", 999)
            put("exportedAt", System.currentTimeMillis())
            put("categories", org.json.JSONArray())
            put("accounts", org.json.JSONArray())
            put("records", org.json.JSONArray())
        }

        val inputStream = ByteArrayInputStream(invalidJson.toString().toByteArray())
        try {
            backupService.importBackup(inputStream)
            fail("Should throw exception for invalid version")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("不支持的备份版本") == true)
        }
    }

    @Test
    fun `export and import round-trip preserves data`() = runBlocking {
        val outputStream = ByteArrayOutputStream()
        backupService.exportBackup(outputStream)

        val exportedJson = outputStream.toString()

        val inputStream = ByteArrayInputStream(exportedJson.toByteArray())
        backupService.importBackup(inputStream)

        val records = repository.getRecords()
        assertTrue(records.isNotEmpty())
    }
}