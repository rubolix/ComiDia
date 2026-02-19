package com.rubolix.comidia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val instructions: String = "",
    val servings: Int = 4,
    val prepTimeMinutes: Int = 0,
    val cookTimeMinutes: Int = 0,
    val imageUri: String? = null,
    val sourceUrl: String? = null,
    val rating: Float = 0f,
    val isKidApproved: Boolean = false,
    val notes: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalTimeMinutes: Int get() = prepTimeMinutes + cookTimeMinutes
}
