package com.rubolix.comidia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "weekly_items")
data class WeeklyItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val weekStartDate: String, // ISO date of Monday
    val text: String,
    val isCompleted: Boolean = false
)
