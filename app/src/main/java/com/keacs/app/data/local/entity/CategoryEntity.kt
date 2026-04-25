package com.keacs.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["direction"]),
        Index(value = ["name", "direction"], unique = true),
    ],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val direction: String,
    val isPreset: Boolean,
    val isEnabled: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
