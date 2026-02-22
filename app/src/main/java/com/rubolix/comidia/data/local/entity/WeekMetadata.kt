package com.rubolix.comidia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "week_metadata")
data class WeekMetadata(
    @PrimaryKey
    val weekStartDate: String, // ISO date
    val availabilityPassDone: Boolean = false
)
