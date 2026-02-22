package com.rubolix.comidia.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "recipe_categories")
data class RecipeCategoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val parentId: String? = null,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "recipe_category_cross_ref",
    primaryKeys = ["recipeId", "categoryId"],
    foreignKeys = [
        ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = RecipeCategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("categoryId")]
)
data class RecipeCategoryCrossRef(
    val recipeId: String,
    val categoryId: String,
    val addedAt: Long = System.currentTimeMillis()
)
