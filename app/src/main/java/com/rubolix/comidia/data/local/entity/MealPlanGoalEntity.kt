package com.rubolix.comidia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "meal_plan_goals")
data class MealPlanGoalEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val description: String, // e.g., "Fish meals"
    val tagId: String? = null, // match by tag
    val categoryId: String? = null, // or match by category
    val goalType: String, // "min" or "max"
    val targetCount: Int, // e.g., 1, 3
    val isActive: Boolean = true
)
