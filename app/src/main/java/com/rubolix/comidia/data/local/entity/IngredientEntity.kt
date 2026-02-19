package com.rubolix.comidia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val recipeId: String,
    val name: String,
    val quantity: String = "",
    val unit: String = "",
    val category: String = ""
)
