package com.rubolix.comidia.data.local.entity

import org.junit.Assert.*
import org.junit.Test

class StapleEntityTest {

    @Test
    fun defaultValues_areCorrect() {
        val staple = StapleEntity(name = "Milk")
        assertEquals("Milk", staple.name)
        assertEquals("", staple.quantity)
        assertEquals("", staple.unit)
        assertEquals("", staple.category)
        assertFalse(staple.isRemoved)
        assertFalse(staple.needsChecking)
        assertFalse(staple.doNotBuy)
        assertTrue(staple.id.isNotBlank())
    }

    @Test
    fun customValues_areRetained() {
        val staple = StapleEntity(
            name = "Eggs",
            quantity = "12",
            unit = "pcs",
            category = "Dairy",
            isRemoved = true,
            needsChecking = true,
            doNotBuy = true
        )
        assertEquals("Eggs", staple.name)
        assertEquals("12", staple.quantity)
        assertEquals("pcs", staple.unit)
        assertEquals("Dairy", staple.category)
        assertTrue(staple.isRemoved)
        assertTrue(staple.needsChecking)
        assertTrue(staple.doNotBuy)
    }
}
