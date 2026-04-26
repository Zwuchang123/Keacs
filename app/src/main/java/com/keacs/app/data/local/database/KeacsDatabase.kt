package com.keacs.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
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
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN iconKey TEXT NOT NULL DEFAULT 'more'")
                db.execSQL("ALTER TABLE categories ADD COLUMN colorKey TEXT NOT NULL DEFAULT 'gray'")
                db.execSQL("ALTER TABLE accounts ADD COLUMN iconKey TEXT NOT NULL DEFAULT 'wallet'")
                db.execSQL("ALTER TABLE accounts ADD COLUMN colorKey TEXT NOT NULL DEFAULT 'gray'")
                updateCategoryIcons(db)
                updateAccountIcons(db)
            }
        }

        private fun updateCategoryIcons(db: SupportSQLiteDatabase) {
            mapOf(
                "餐饮" to ("food" to "orange"),
                "交通" to ("bus" to "blue"),
                "日用" to ("bag" to "green"),
                "住房" to ("home" to "cyan"),
                "水电煤" to ("bolt" to "yellow"),
                "通讯" to ("phone" to "indigo"),
                "医疗" to ("medical" to "red"),
                "娱乐" to ("game" to "purple"),
                "教育" to ("school" to "yellow"),
                "投资" to ("chart" to "green"),
                "人情" to ("heart" to "pink"),
                "工资" to ("work" to "blue"),
                "奖金" to ("gift" to "yellow"),
                "报销" to ("receipt" to "green"),
                "理财收益" to ("chart" to "purple"),
                "礼金" to ("gift" to "orange"),
                "兼职" to ("coins" to "cyan"),
            ).forEach { (name, icon) ->
                db.execSQL(
                    "UPDATE categories SET iconKey = ?, colorKey = ? WHERE name = ?",
                    arrayOf(icon.first, icon.second, name),
                )
            }
        }

        private fun updateAccountIcons(db: SupportSQLiteDatabase) {
            mapOf(
                "现金" to ("wallet" to "green"),
                "支付宝" to ("wallet" to "blue"),
                "微信" to ("wallet" to "cyan"),
                "银行卡" to ("bank" to "indigo"),
                "信用卡" to ("card" to "orange"),
                "花呗/白条" to ("card" to "yellow"),
                "公积金" to ("home" to "purple"),
                "投资账户" to ("chart" to "pink"),
                "消费贷" to ("loan" to "red"),
                "房贷/车贷" to ("loan" to "red"),
                "亲友借款" to ("loan" to "red"),
            ).forEach { (name, icon) ->
                db.execSQL(
                    "UPDATE accounts SET iconKey = ?, colorKey = ? WHERE name = ?",
                    arrayOf(icon.first, icon.second, name),
                )
            }
        }
    }
}
