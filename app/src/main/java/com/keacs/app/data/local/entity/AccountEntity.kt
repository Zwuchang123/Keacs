package com.keacs.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["nature"]),
        Index(value = ["name"], unique = true),
    ],
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nature: String,
    val type: String,
    val initialBalanceCent: Long,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
