package com.rubolix.comidia.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "meal_slot_custom_entries",
    foreignKeys = [
        ForeignKey(
            entity = MealSlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealSlotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mealSlotId")]
)
data class MealSlotCustomEntry(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val mealSlotId: String,
    val title: String,
    val type: String, // "takeout", "freezer", "eating_out", "pantry", "other"
    val sortOrder: Int = 0,
    val isLeftover: Boolean = false,
    val generatesLeftovers: Boolean = false,
    val fromFreezer: Boolean = false,
    val servings: Int? = null
)
