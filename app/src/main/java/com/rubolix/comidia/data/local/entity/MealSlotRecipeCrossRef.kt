package com.rubolix.comidia.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "meal_slot_recipes",
    primaryKeys = ["mealSlotId", "recipeId"],
    foreignKeys = [
        ForeignKey(entity = MealSlotEntity::class, parentColumns = ["id"], childColumns = ["mealSlotId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("recipeId")]
)
data class MealSlotRecipeCrossRef(
    val mealSlotId: String,
    val recipeId: String,
    val sortOrder: Int = 0
)
