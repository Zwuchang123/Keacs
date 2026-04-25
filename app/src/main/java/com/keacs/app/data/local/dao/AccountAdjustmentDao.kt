package com.keacs.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.keacs.app.data.local.entity.AccountAdjustmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountAdjustmentDao {
    @Insert
    suspend fun insert(adjustment: AccountAdjustmentEntity): Long

    @Query("SELECT * FROM account_adjustments ORDER BY occurredAt DESC, id DESC")
    fun observeAll(): Flow<List<AccountAdjustmentEntity>>

    @Query("SELECT * FROM account_adjustments ORDER BY occurredAt DESC, id DESC")
    suspend fun getAll(): List<AccountAdjustmentEntity>

    @Query("SELECT COUNT(*) FROM account_adjustments")
    suspend fun count(): Int
}
