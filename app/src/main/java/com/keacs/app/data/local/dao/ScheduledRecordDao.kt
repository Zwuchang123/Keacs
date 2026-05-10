package com.keacs.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.keacs.app.data.local.entity.ScheduledRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledRecordDao {
    @Insert
    suspend fun insert(record: ScheduledRecordEntity): Long

    @Update
    suspend fun update(record: ScheduledRecordEntity)

    @Query("SELECT * FROM scheduled_records ORDER BY isEnabled DESC, nextRunAt ASC, id DESC")
    fun observeAll(): Flow<List<ScheduledRecordEntity>>

    @Query("SELECT * FROM scheduled_records ORDER BY isEnabled DESC, nextRunAt ASC, id DESC")
    suspend fun getAll(): List<ScheduledRecordEntity>

    @Query("SELECT * FROM scheduled_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScheduledRecordEntity?

    @Query("DELETE FROM scheduled_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
