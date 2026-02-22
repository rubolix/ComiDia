package com.rubolix.comidia.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "category_smart_rules",
    foreignKeys = [
        ForeignKey(
            entity = RecipeCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class CategorySmartRuleEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val minStars: Int = 0,
    val kidApprovedOnly: Boolean = false,
    val maxTotalTime: Int? = null,
    val minTotalTime: Int? = null,
    val maxPrepTime: Int? = null,
    val includeTagIds: String = "", // Comma-separated
    val includeCategoryIds: String = "", // Comma-separated (Recursive)
    val createdAt: Long = System.currentTimeMillis()
)
