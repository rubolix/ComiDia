package com.rubolix.comidia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_ingredient_preferences")
data class UserIngredientPreference(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val weekStartDate: String, // ISO date of the Monday
    val ingredientName: String, // Normalize to lowercase
    val recipeId: String? = null, // Optional: if null, applies to all instances of this ingredient in the week
    val doNotBuy: Boolean = false,
    val isRemoved: Boolean = false,
    val needsChecking: Boolean = false,
    val isPurchased: Boolean = false
)
