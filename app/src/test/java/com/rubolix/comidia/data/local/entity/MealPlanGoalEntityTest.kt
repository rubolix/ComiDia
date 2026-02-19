package com.rubolix.comidia.data.local.entity

import org.junit.Assert.*
import org.junit.Test

class MealPlanGoalEntityTest {

    @Test
    fun defaultPeriod_isWeek() {
        val goal = MealPlanGoalEntity(
            description = "Fish",
            goalType = "eq",
            targetCount = 1
        )
        assertEquals("week", goal.period)
    }

    @Test
    fun defaultIsActive_isTrue() {
        val goal = MealPlanGoalEntity(
            description = "Fish",
            goalType = "eq",
            targetCount = 1
        )
        assertTrue(goal.isActive)
    }

    @Test
    fun goalType_supportsAllValues() {
        listOf("eq", "gte", "lte").forEach { type ->
            val goal = MealPlanGoalEntity(
                description = "Test",
                goalType = type,
                targetCount = 2
            )
            assertEquals(type, goal.goalType)
        }
    }

    @Test
    fun period_supportsAllValues() {
        listOf("day", "week", "month").forEach { period ->
            val goal = MealPlanGoalEntity(
                description = "Test",
                goalType = "eq",
                targetCount = 1,
                period = period
            )
            assertEquals(period, goal.period)
        }
    }

    @Test
    fun tagId_canBeNull() {
        val goal = MealPlanGoalEntity(
            description = "Test",
            goalType = "gte",
            targetCount = 3
        )
        assertNull(goal.tagId)
    }

    @Test
    fun categoryId_canBeNull() {
        val goal = MealPlanGoalEntity(
            description = "Test",
            goalType = "lte",
            targetCount = 2
        )
        assertNull(goal.categoryId)
    }
}
