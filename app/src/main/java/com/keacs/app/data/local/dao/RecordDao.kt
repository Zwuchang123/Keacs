package com.keacs.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.keacs.app.data.local.entity.RecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Insert
    suspend fun insert(record: RecordEntity): Long

    @Query("SELECT * FROM records ORDER BY occurredAt DESC, id DESC")
    fun observeAll(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records ORDER BY occurredAt DESC, id DESC")
    suspend fun getAll(): List<RecordEntity>

    @Query("SELECT COUNT(*) FROM records")
    suspend fun count(): Int
}
