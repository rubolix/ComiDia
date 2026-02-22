package com.rubolix.comidia.data.local.entity

data class ShoppingItem(
    val name: String,
    val quantity: String,
    val unit: String,
    val category: String,
    val recipeNames: List<String>,
    val dayLabel: String = "",
    val doNotBuy: Boolean = false,
    val isRemoved: Boolean = false,
    val needsChecking: Boolean = false,
    val isPurchased: Boolean = false
)
