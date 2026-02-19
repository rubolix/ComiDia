package com.rubolix.comidia.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "meal_slots",
    foreignKeys = [
        ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("recipeId"), Index("date", "mealType", unique = true)]
)
data class MealSlotEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val date: String, // ISO date: "2026-02-19"
    val mealType: String, // "breakfast", "lunch", "dinner"
    val recipeId: String? = null
)
