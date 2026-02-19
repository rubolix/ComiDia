package com.rubolix.comidia.data.local.entity

import org.junit.Assert.*
import org.junit.Test

class IngredientEntityTest {

    @Test
    fun defaultValues_areCorrect() {
        val ingredient = IngredientEntity(recipeId = "recipe-1", name = "Salt")
        assertEquals("Salt", ingredient.name)
        assertEquals("recipe-1", ingredient.recipeId)
        assertEquals("", ingredient.quantity)
        assertEquals("", ingredient.unit)
        assertEquals("", ingredient.category)
        assertTrue(ingredient.id.isNotBlank())
    }

    @Test
    fun fullConstructor_setsAllFields() {
        val ingredient = IngredientEntity(
            recipeId = "recipe-1",
            name = "Olive oil",
            quantity = "2",
            unit = "tbsp",
            category = "Pantry"
        )
        assertEquals("Olive oil", ingredient.name)
        assertEquals("2", ingredient.quantity)
        assertEquals("tbsp", ingredient.unit)
        assertEquals("Pantry", ingredient.category)
    }
}
