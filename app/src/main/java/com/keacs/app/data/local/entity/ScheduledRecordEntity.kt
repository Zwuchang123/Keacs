package com.keacs.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scheduled_records",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromAccountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["type"]),
        Index(value = ["categoryId"]),
        Index(value = ["fromAccountId"]),
        Index(value = ["toAccountId"]),
        Index(value = ["nextRunAt"]),
    ],
)
data class ScheduledRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amountCent: Long,
    val categoryId: Long?,
    val fromAccountId: Long?,
    val toAccountId: Long?,
    val frequency: String,
    val recurrenceMonth: Int?,
    val recurrenceDay: Int?,
    val recurrenceWeekday: Int?,
    val recurrenceValues: String?,
    val recurrenceHour: Int,
    val nextRunAt: Long,
    val note: String?,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
