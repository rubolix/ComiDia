package com.rubolix.comidia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "daily_todos")
data class DailyTodoEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val date: String, // ISO date
    val text: String,
    val isCompleted: Boolean = false
)
