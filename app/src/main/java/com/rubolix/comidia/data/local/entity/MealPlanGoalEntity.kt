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
    val goalType: String, // "eq", "gte", "lte" (equal, at least, at most)
    val targetCount: Int, // e.g., 1, 3
    val period: String = "week", // "day", "week", "month"
    val isActive: Boolean = true
)
