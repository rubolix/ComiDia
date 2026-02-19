package com.rubolix.comidia.data.local.entity

import org.junit.Assert.*
import org.junit.Test

class RecipeEntityTest {

    @Test
    fun totalTimeMinutes_sumsPrepAndCook() {
        val recipe = RecipeEntity(name = "Test", prepTimeMinutes = 15, cookTimeMinutes = 30)
        assertEquals(45, recipe.totalTimeMinutes)
    }

    @Test
    fun totalTimeMinutes_zeroWhenBothZero() {
        val recipe = RecipeEntity(name = "Test")
        assertEquals(0, recipe.totalTimeMinutes)
    }

    @Test
    fun totalTimeMinutes_onlyPrepTime() {
        val recipe = RecipeEntity(name = "Test", prepTimeMinutes = 20, cookTimeMinutes = 0)
        assertEquals(20, recipe.totalTimeMinutes)
    }

    @Test
    fun totalTimeMinutes_onlyCookTime() {
        val recipe = RecipeEntity(name = "Test", prepTimeMinutes = 0, cookTimeMinutes = 45)
        assertEquals(45, recipe.totalTimeMinutes)
    }

    @Test
    fun defaultValues_areCorrect() {
        val recipe = RecipeEntity(name = "Test Recipe")
        assertEquals("Test Recipe", recipe.name)
        assertEquals("", recipe.instructions)
        assertEquals(4, recipe.servings)
        assertEquals(0, recipe.prepTimeMinutes)
        assertEquals(0, recipe.cookTimeMinutes)
        assertNull(recipe.imageUri)
        assertNull(recipe.sourceUrl)
        assertEquals(0f, recipe.rating, 0.001f)
        assertFalse(recipe.isKidApproved)
        assertEquals("", recipe.notes)
        assertFalse(recipe.isArchived)
        assertTrue(recipe.id.isNotBlank())
    }

    @Test
    fun rating_acceptsHalfStars() {
        val recipe = RecipeEntity(name = "Test", rating = 3.5f)
        assertEquals(3.5f, recipe.rating, 0.001f)
    }
}
