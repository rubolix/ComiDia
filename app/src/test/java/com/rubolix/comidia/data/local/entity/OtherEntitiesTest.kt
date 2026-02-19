package com.rubolix.comidia.data.local.entity

import org.junit.Assert.*
import org.junit.Test

class TagEntityTest {

    @Test
    fun defaultColor_isSet() {
        val tag = TagEntity(name = "Vegetarian")
        assertEquals("Vegetarian", tag.name)
        assertTrue(tag.id.isNotBlank())
        // Default color should be non-zero
        assertNotEquals(0L, tag.color)
    }

    @Test
    fun customColor_isPreserved() {
        val tag = TagEntity(name = "Fish", color = 0xFF00FF00)
        assertEquals(0xFF00FF00, tag.color)
    }
}

class MealSlotEntityTest {

    @Test
    fun constructionWorks() {
        val slot = MealSlotEntity(date = "2024-01-15", mealType = "dinner")
        assertEquals("2024-01-15", slot.date)
        assertEquals("dinner", slot.mealType)
        assertTrue(slot.id.isNotBlank())
    }
}

class DailyTodoEntityTest {

    @Test
    fun defaultValues_areCorrect() {
        val todo = DailyTodoEntity(date = "2024-01-15", text = "Defrost chicken")
        assertEquals("2024-01-15", todo.date)
        assertEquals("Defrost chicken", todo.text)
        assertFalse(todo.isCompleted)
    }
}

class WeeklyItemEntityTest {

    @Test
    fun defaultValues_areCorrect() {
        val item = WeeklyItemEntity(weekStartDate = "2024-01-15", text = "Buy fruit")
        assertEquals("2024-01-15", item.weekStartDate)
        assertEquals("Buy fruit", item.text)
        assertFalse(item.isCompleted)
    }
}

class RecipeTagCrossRefTest {

    @Test
    fun constructionWorks() {
        val ref = RecipeTagCrossRef(recipeId = "r1", tagId = "t1")
        assertEquals("r1", ref.recipeId)
        assertEquals("t1", ref.tagId)
    }
}

class MealSlotRecipeCrossRefTest {

    @Test
    fun defaultSortOrder_isZero() {
        val ref = MealSlotRecipeCrossRef(mealSlotId = "s1", recipeId = "r1")
        assertEquals(0, ref.sortOrder)
    }

    @Test
    fun sortOrder_canBeSet() {
        val ref = MealSlotRecipeCrossRef(mealSlotId = "s1", recipeId = "r1", sortOrder = 3)
        assertEquals(3, ref.sortOrder)
    }
}
