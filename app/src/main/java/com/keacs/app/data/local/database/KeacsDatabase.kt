package com.keacs.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.keacs.app.data.local.dao.AccountAdjustmentDao
import com.keacs.app.data.local.dao.AccountDao
import com.keacs.app.data.local.dao.AppMetaDao
import com.keacs.app.data.local.dao.CategoryDao
import com.keacs.app.data.local.dao.RecordDao
import com.keacs.app.data.local.entity.AccountAdjustmentEntity
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.AppMetaEntity
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.local.entity.RecordEntity

@Database(
    entities = [
        CategoryEntity::class,
        AccountEntity::class,
        RecordEntity::class,
        AccountAdjustmentEntity::class,
        AppMetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class KeacsDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun recordDao(): RecordDao
    abstract fun accountAdjustmentDao(): AccountAdjustmentDao
    abstract fun appMetaDao(): AppMetaDao

    companion object {
        const val DATABASE_NAME = "keacs.db"

        @Volatile
        private var instance: KeacsDatabase? = null

        fun getInstance(context: Context): KeacsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KeacsDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }
        }
    }
}
