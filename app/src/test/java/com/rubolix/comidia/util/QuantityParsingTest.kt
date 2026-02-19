package com.rubolix.comidia.util

import org.junit.Assert.*
import org.junit.Test

class QuantityParsingTest {

    // Mirror the parsing logic from IngredientsViewModel
    private fun parseFraction(s: String): Double? {
        val trimmed = s.trim()
        if (trimmed.isBlank()) return null
        trimmed.toDoubleOrNull()?.let { return it }
        val parts = trimmed.split("/")
        if (parts.size == 2) {
            val num = parts[0].trim().toDoubleOrNull() ?: return null
            val den = parts[1].trim().toDoubleOrNull() ?: return null
            if (den != 0.0) return num / den
        }
        return null
    }

    private fun combineQuantities(quantities: List<String>): String {
        val nums = quantities.mapNotNull { parseFraction(it) }
        return if (nums.size == quantities.size && nums.isNotEmpty()) {
            val sum = nums.sum()
            if (sum == sum.toInt().toDouble()) sum.toInt().toString()
            else String.format("%.1f", sum)
        } else {
            quantities.filter { it.isNotBlank() }.joinToString(" + ")
        }
    }

    @Test
    fun parseFraction_wholeNumber() {
        assertEquals(3.0, parseFraction("3")!!, 0.001)
    }

    @Test
    fun parseFraction_decimal() {
        assertEquals(1.5, parseFraction("1.5")!!, 0.001)
    }

    @Test
    fun parseFraction_fraction() {
        assertEquals(0.5, parseFraction("1/2")!!, 0.001)
    }

    @Test
    fun parseFraction_quarterFraction() {
        assertEquals(0.25, parseFraction("1/4")!!, 0.001)
    }

    @Test
    fun parseFraction_blank_returnsNull() {
        assertNull(parseFraction(""))
        assertNull(parseFraction("  "))
    }

    @Test
    fun parseFraction_nonNumeric_returnsNull() {
        assertNull(parseFraction("a bunch"))
    }

    @Test
    fun parseFraction_divisionByZero_returnsNull() {
        assertNull(parseFraction("1/0"))
    }

    @Test
    fun combineQuantities_sumsWholeNumbers() {
        assertEquals("5", combineQuantities(listOf("2", "3")))
    }

    @Test
    fun combineQuantities_sumsFractions() {
        assertEquals("1", combineQuantities(listOf("1/2", "1/2")))
    }

    @Test
    fun combineQuantities_sumsDecimals() {
        // 1.5 + 1.5 = 3.0, which is a whole number so formatted as "3"
        assertEquals("3", combineQuantities(listOf("1.5", "1.5")))
    }

    @Test
    fun combineQuantities_mixedNumericTypes() {
        assertEquals("2.5", combineQuantities(listOf("2", "1/2")))
    }

    @Test
    fun combineQuantities_nonNumeric_joinsPlusSign() {
        assertEquals("a pinch + some", combineQuantities(listOf("a pinch", "some")))
    }

    @Test
    fun combineQuantities_singleValue() {
        assertEquals("3", combineQuantities(listOf("3")))
    }

    @Test
    fun combineQuantities_emptyList_returnsEmpty() {
        assertEquals("", combineQuantities(emptyList()))
    }
}
