package com.rubolix.comidia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val key: String,
    val value: String
) {
    companion object {
        const val FIRST_DAY_OF_WEEK = "first_day_of_week" // "monday" or "sunday"
        const val DEFAULT_MEAL_TYPES = "default_meal_types" // comma-separated: "dinner" or "breakfast,dinner"
    }
}
