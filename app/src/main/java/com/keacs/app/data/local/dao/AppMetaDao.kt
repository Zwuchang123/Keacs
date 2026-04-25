package com.keacs.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.keacs.app.data.local.entity.AppMetaEntity

@Dao
interface AppMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: AppMetaEntity)

    @Query("SELECT * FROM app_meta WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): AppMetaEntity?
}
