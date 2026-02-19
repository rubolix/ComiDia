package com.rubolix.comidia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "staples")
data class StapleEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: String = "",
    val unit: String = "",
    val category: String = "",
    val isRemoved: Boolean = false,
    val needsChecking: Boolean = false,
    val doNotBuy: Boolean = false,
    val isPurchased: Boolean = false
)
