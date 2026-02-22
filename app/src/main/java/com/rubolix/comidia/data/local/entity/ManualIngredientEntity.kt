package com.rubolix.comidia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "manual_ingredients")
data class ManualIngredientEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val weekStartDate: String, // ISO date
    val name: String,
    val quantity: String = "",
    val unit: String = "",
    val category: String = ""
)
