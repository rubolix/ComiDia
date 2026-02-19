package com.rubolix.comidia.util

import org.junit.Assert.*
import org.junit.Test

class IngredientUtilsTest {

    @Test
    fun normalizeIngredientName_stripsPrepWords() {
        assertEquals("onion", IngredientUtils.normalizeIngredientName("Chopped Onion"))
        assertEquals("onion", IngredientUtils.normalizeIngredientName("sliced, diced onions"))
        assertEquals("garlic clove", IngredientUtils.normalizeIngredientName("minced garlic cloves"))
    }

    @Test
    fun isWater_identifiesWater() {
        assertTrue(IngredientUtils.isWater("Water"))
        assertTrue(IngredientUtils.isWater("agua"))
        assertTrue(IngredientUtils.isWater("Fresh Water"))
        assertFalse(IngredientUtils.isWater("Onion"))
    }

    @Test
    fun combineQuantities_sumsFractions() {
        val qty = listOf("1/2", "1", "0.5")
        val scales = listOf(1.0, 1.0, 1.0)
        assertEquals("2", IngredientUtils.combineQuantities(qty, scales))
    }

    @Test
    fun combineQuantities_handlesMixedUnits() {
        val qty = listOf("1", "large")
        val scales = listOf(1.0, 1.0)
        assertEquals("1 + large", IngredientUtils.combineQuantities(qty, scales))
    }
}
