package com.keacs.app.data.backup

import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.local.entity.RecordEntity
import com.keacs.app.data.repository.LocalDataRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

class BackupService(
    private val localDataRepository: LocalDataRepository
) {
    suspend fun exportBackup(outputStream: OutputStream) {
        val categories = localDataRepository.getCategories()
        val accounts = localDataRepository.getAccounts()
        val records = localDataRepository.getRecords()

        val root = JSONObject()
        root.put("backupVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val categoriesArray = JSONArray()
        categories.forEach { cat ->
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            obj.put("direction", cat.direction)
            obj.put("iconKey", cat.iconKey)
            obj.put("colorKey", cat.colorKey)
            obj.put("isPreset", cat.isPreset)
            obj.put("isEnabled", cat.isEnabled)
            obj.put("sortOrder", cat.sortOrder)
            obj.put("createdAt", cat.createdAt)
            obj.put("updatedAt", cat.updatedAt)
            categoriesArray.put(obj)
        }
        root.put("categories", categoriesArray)

        val accountsArray = JSONArray()
        accounts.forEach { acc ->
            val obj = JSONObject()
            obj.put("id", acc.id)
            obj.put("name", acc.name)
            obj.put("nature", acc.nature)
            obj.put("type", acc.type)
            obj.put("iconKey", acc.iconKey)
            obj.put("colorKey", acc.colorKey)
            obj.put("initialBalanceCent", acc.initialBalanceCent)
            obj.put("isEnabled", acc.isEnabled)
            obj.put("createdAt", acc.createdAt)
            obj.put("updatedAt", acc.updatedAt)
            accountsArray.put(obj)
        }
        root.put("accounts", accountsArray)

        val recordsArray = JSONArray()
        records.forEach { rec ->
            val obj = JSONObject()
            obj.put("id", rec.id)
            obj.put("type", rec.type)
            obj.put("amountCent", rec.amountCent)
            obj.put("occurredAt", rec.occurredAt)
            obj.put("categoryId", rec.categoryId ?: JSONObject.NULL)
            obj.put("fromAccountId", rec.fromAccountId ?: JSONObject.NULL)
            obj.put("toAccountId", rec.toAccountId ?: JSONObject.NULL)
            obj.put("note", rec.note ?: JSONObject.NULL)
            obj.put("createdAt", rec.createdAt)
            obj.put("updatedAt", rec.updatedAt)
            recordsArray.put(obj)
        }
        root.put("records", recordsArray)

        outputStream.writer().use {
            it.write(root.toString(2))
        }
    }

    suspend fun importBackup(inputStream: InputStream) {
        val jsonString = inputStream.reader().use { it.readText() }
        val root = JSONObject(jsonString)

        val version = root.optInt("backupVersion", 0)
        require(version == 1) { "不支持的备份版本或文件格式错误" }

        val categoriesArray = root.optJSONArray("categories") ?: JSONArray()
        val accountsArray = root.optJSONArray("accounts") ?: JSONArray()
        val recordsArray = root.optJSONArray("records") ?: JSONArray()

        val categories = mutableListOf<CategoryEntity>()
        for (i in 0 until categoriesArray.length()) {
            val obj = categoriesArray.getJSONObject(i)
            categories.add(
                CategoryEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    direction = obj.getString("direction"),
                    iconKey = obj.getString("iconKey"),
                    colorKey = obj.getString("colorKey"),
                    isPreset = obj.getBoolean("isPreset"),
                    isEnabled = obj.getBoolean("isEnabled"),
                    sortOrder = obj.getInt("sortOrder"),
                    createdAt = obj.getLong("createdAt"),
                    updatedAt = obj.getLong("updatedAt")
                )
            )
        }

        val accounts = mutableListOf<AccountEntity>()
        for (i in 0 until accountsArray.length()) {
            val obj = accountsArray.getJSONObject(i)
            accounts.add(
                AccountEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    nature = obj.getString("nature"),
                    type = obj.getString("type"),
                    iconKey = obj.getString("iconKey"),
                    colorKey = obj.getString("colorKey"),
                    initialBalanceCent = obj.getLong("initialBalanceCent"),
                    isEnabled = obj.getBoolean("isEnabled"),
                    createdAt = obj.getLong("createdAt"),
                    updatedAt = obj.getLong("updatedAt")
                )
            )
        }

        val records = mutableListOf<RecordEntity>()
        for (i in 0 until recordsArray.length()) {
            val obj = recordsArray.getJSONObject(i)
            records.add(
                RecordEntity(
                    id = obj.getLong("id"),
                    type = obj.getString("type"),
                    amountCent = obj.getLong("amountCent"),
                    occurredAt = obj.getLong("occurredAt"),
                    categoryId = if (obj.isNull("categoryId")) null else obj.getLong("categoryId"),
                    fromAccountId = if (obj.isNull("fromAccountId")) null else obj.getLong("fromAccountId"),
                    toAccountId = if (obj.isNull("toAccountId")) null else obj.getLong("toAccountId"),
                    note = if (obj.isNull("note")) null else obj.getString("note"),
                    createdAt = obj.getLong("createdAt"),
                    updatedAt = obj.getLong("updatedAt")
                )
            )
        }

        localDataRepository.importBackup(categories, accounts, records)
    }
}
