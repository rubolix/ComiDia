package com.rubolix.comidia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun DialogUnderlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.1f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    )
}

/**
 * A simple data structure to represent a tree of categories.
 */
data class CategoryNode(
    val category: com.rubolix.comidia.data.local.entity.RecipeCategoryEntity,
    val children: List<CategoryNode> = emptyList()
)

object CategoryTreeUtils {
    fun buildTree(
        allCategories: List<com.rubolix.comidia.data.local.entity.RecipeCategoryEntity>,
        parentId: String? = null
    ): List<CategoryNode> {
        return allCategories
            .filter { it.parentId == parentId }
            .sortedBy { it.sortOrder }
            .map { CategoryNode(it, buildTree(allCategories, it.id)) }
    }
}
