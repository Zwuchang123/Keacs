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
import com.keacs.app.data.local.dao.ScheduledRecordDao
import com.keacs.app.data.local.entity.AccountAdjustmentEntity
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.AppMetaEntity
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.local.entity.RecordEntity
import com.keacs.app.data.local.entity.ScheduledRecordEntity

@Database(
    entities = [
        CategoryEntity::class,
        AccountEntity::class,
        RecordEntity::class,
        AccountAdjustmentEntity::class,
        AppMetaEntity::class,
        ScheduledRecordEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class KeacsDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun recordDao(): RecordDao
    abstract fun scheduledRecordDao(): ScheduledRecordDao
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS scheduled_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        amountCent INTEGER NOT NULL,
                        categoryId INTEGER,
                        fromAccountId INTEGER,
                        toAccountId INTEGER,
                        frequency TEXT NOT NULL,
                        recurrenceMonth INTEGER,
                        recurrenceDay INTEGER,
                        recurrenceWeekday INTEGER,
                        recurrenceHour INTEGER NOT NULL DEFAULT 0,
                        nextRunAt INTEGER NOT NULL,
                        note TEXT,
                        isEnabled INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(fromAccountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(toAccountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_records_type ON scheduled_records(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_records_categoryId ON scheduled_records(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_records_fromAccountId ON scheduled_records(fromAccountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_records_toAccountId ON scheduled_records(toAccountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_records_nextRunAt ON scheduled_records(nextRunAt)")
                db.execSQL("UPDATE categories SET iconKey = 'bonus' WHERE direction = 'INCOME' AND name = '奖金'")
                db.execSQL("UPDATE accounts SET iconKey = 'mortgage', colorKey = 'blue' WHERE name = '房贷/车贷'")
                db.execSQL("UPDATE accounts SET iconKey = 'request_account', colorKey = 'green' WHERE name = '亲友借款'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = mutableSetOf<String>()
                db.query("PRAGMA table_info(scheduled_records)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        columns.add(cursor.getString(nameIndex))
                    }
                }
                val fromAccountExpr = if ("fromAccountId" in columns) {
                    "fromAccountId"
                } else {
                    "CASE WHEN type = 'EXPENSE' THEN accountId ELSE NULL END"
                }
                val toAccountExpr = if ("toAccountId" in columns) {
                    "toAccountId"
                } else {
                    "CASE WHEN type = 'INCOME' THEN accountId ELSE NULL END"
                }
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS scheduled_records_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        amountCent INTEGER NOT NULL,
                        categoryId INTEGER,
                        fromAccountId INTEGER,
                        toAccountId INTEGER,
                        frequency TEXT NOT NULL,
                        recurrenceMonth INTEGER,
                        recurrenceDay INTEGER,
                        recurrenceWeekday INTEGER,
                        recurrenceHour INTEGER NOT NULL DEFAULT 0,
                        nextRunAt INTEGER NOT NULL,
                        note TEXT,
                        isEnabled INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(fromAccountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(toAccountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO scheduled_records_new (
                        id, type, amountCent, categoryId, fromAccountId, toAccountId, frequency,
                        recurrenceMonth, recurrenceDay, recurrenceWeekday, recurrenceHour,
                        nextRunAt, note, isEnabled, createdAt, updatedAt
                    )
                    SELECT id, type, amountCent, categoryId, $fromAccountExpr, $toAccountExpr, frequency,
                        NULL, NULL, NULL, 0, nextRunAt, note, isEnabled, createdAt, updatedAt
                    FROM scheduled_records
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE scheduled_records")
                db.execSQL("ALTER TABLE scheduled_records_new RENAME TO scheduled_records")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_records_type ON scheduled_records(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_records_categoryId ON scheduled_records(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_records_fromAccountId ON scheduled_records(fromAccountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_records_toAccountId ON scheduled_records(toAccountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_records_nextRunAt ON scheduled_records(nextRunAt)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scheduled_records ADD COLUMN recurrenceValues TEXT")
                db.execSQL("UPDATE scheduled_records SET frequency = 'WEEKLY' WHERE frequency = 'DAILY'")
                db.execSQL("UPDATE scheduled_records SET recurrenceHour = 9")
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
                "奖金" to ("bonus" to "yellow"),
                "报销" to ("receipt" to "green"),
                "理财收益" to ("profit_chart" to "purple"),
                "礼金" to ("gift" to "orange"),
                "兼职" to ("coins" to "cyan"),
            ).forEach { (name, icon) ->
                db.execSQL(
                    "UPDATE categories SET iconKey = ?, colorKey = ? WHERE name = ? AND updatedAt = createdAt",
                    arrayOf(icon.first, icon.second, name),
                )
            }
            mapOf(
                "支付宝" to ("alipay" to "blue"),
                "微信" to ("wechat_asset" to "cyan"),
                "现金" to ("cash_asset" to "green"),
                "银行卡" to ("bank_asset" to "indigo"),
                "公积金" to ("housing_fund" to "purple"),
                "投资账户" to ("investment_asset" to "pink"),
                "其他资产" to ("asset_more" to "gray"),
                "信用卡" to ("credit_card_liability" to "orange"),
                "花呗白条" to ("credit_line" to "yellow"),
                "消费贷" to ("consumer_loan" to "red"),
                "房贷车贷" to ("mortgage_liability" to "blue"),
                "亲友借款" to ("friend_loan" to "green"),
                "其他负债" to ("liability_more" to "gray"),
            ).forEach { (name, icon) ->
                db.execSQL(
                    "UPDATE categories SET iconKey = ?, colorKey = ? WHERE name = ? AND updatedAt = createdAt",
                    arrayOf(icon.first, icon.second, name),
                )
            }
            db.execSQL(
                "UPDATE categories SET iconKey = 'income_more', colorKey = 'gray' WHERE direction = 'INCOME' AND name = '其他' AND updatedAt = createdAt",
            )
            db.execSQL(
                "UPDATE categories SET iconKey = 'more', colorKey = 'gray' WHERE direction = 'EXPENSE' AND name = '其他' AND updatedAt = createdAt",
            )
        }

        private fun updateAccountIcons(db: SupportSQLiteDatabase) {
            mapOf(
                "现金" to ("cash_asset" to "green"),
                "支付宝" to ("alipay" to "blue"),
                "微信" to ("wechat_asset" to "cyan"),
                "银行卡" to ("bank_asset" to "indigo"),
                "信用卡" to ("credit_card_liability" to "orange"),
                "花呗/白条" to ("credit_line" to "yellow"),
                "公积金" to ("housing_fund" to "purple"),
                "投资账户" to ("investment_asset" to "pink"),
                "消费贷" to ("consumer_loan" to "red"),
                "房贷/车贷" to ("mortgage_liability" to "blue"),
                "亲友借款" to ("friend_loan" to "green"),
                "其他资产" to ("asset_more" to "gray"),
                "其他负债" to ("liability_more" to "gray"),
            ).forEach { (name, icon) ->
                db.execSQL(
                    "UPDATE accounts SET iconKey = ?, colorKey = ? WHERE name = ? AND updatedAt = createdAt",
                    arrayOf(icon.first, icon.second, name),
                )
            }
        }
    }
}
