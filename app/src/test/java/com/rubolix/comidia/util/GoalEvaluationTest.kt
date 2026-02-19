package com.rubolix.comidia.util

import com.rubolix.comidia.data.local.entity.MealPlanGoalEntity
import org.junit.Assert.*
import org.junit.Test

class GoalEvaluationTest {

    // Mirror the goal evaluation logic from MenuViewModel
    private fun evaluateGoal(goalType: String, currentCount: Int, targetCount: Int): Boolean {
        return when (goalType) {
            "gte" -> currentCount >= targetCount
            "lte" -> currentCount <= targetCount
            "eq" -> currentCount == targetCount
            "min" -> currentCount >= targetCount
            "max" -> currentCount <= targetCount
            else -> true
        }
    }

    @Test
    fun eq_met_whenEqual() {
        assertTrue(evaluateGoal("eq", 2, 2))
    }

    @Test
    fun eq_notMet_whenNotEqual() {
        assertFalse(evaluateGoal("eq", 1, 2))
        assertFalse(evaluateGoal("eq", 3, 2))
    }

    @Test
    fun gte_met_whenGreaterOrEqual() {
        assertTrue(evaluateGoal("gte", 2, 2))
        assertTrue(evaluateGoal("gte", 3, 2))
    }

    @Test
    fun gte_notMet_whenLess() {
        assertFalse(evaluateGoal("gte", 1, 2))
    }

    @Test
    fun lte_met_whenLessOrEqual() {
        assertTrue(evaluateGoal("lte", 2, 2))
        assertTrue(evaluateGoal("lte", 1, 2))
    }

    @Test
    fun lte_notMet_whenGreater() {
        assertFalse(evaluateGoal("lte", 3, 2))
    }

    @Test
    fun legacyMin_behaves_likeGte() {
        assertTrue(evaluateGoal("min", 2, 1))
        assertTrue(evaluateGoal("min", 1, 1))
        assertFalse(evaluateGoal("min", 0, 1))
    }

    @Test
    fun legacyMax_behaves_likeLte() {
        assertTrue(evaluateGoal("max", 1, 2))
        assertTrue(evaluateGoal("max", 2, 2))
        assertFalse(evaluateGoal("max", 3, 2))
    }

    @Test
    fun unknown_goalType_returnsTrue() {
        assertTrue(evaluateGoal("unknown", 5, 3))
    }

    @Test
    fun goalEntity_withAllComparisons() {
        val goals = listOf(
            MealPlanGoalEntity(description = "Fish", goalType = "gte", targetCount = 1),
            MealPlanGoalEntity(description = "Pasta", goalType = "lte", targetCount = 2),
            MealPlanGoalEntity(description = "Veggie", goalType = "eq", targetCount = 3)
        )

        // Simulate: 1 fish, 1 pasta, 3 veggie
        val counts = mapOf("Fish" to 1, "Pasta" to 1, "Veggie" to 3)

        goals.forEach { goal ->
            val count = counts[goal.description] ?: 0
            val met = evaluateGoal(goal.goalType, count, goal.targetCount)
            assertTrue("Goal '${goal.description}' should be met", met)
        }
    }

    @Test
    fun goalEntity_unmetGoals() {
        val goal = MealPlanGoalEntity(description = "Fish", goalType = "gte", targetCount = 2)
        assertFalse(evaluateGoal(goal.goalType, 1, goal.targetCount))
    }
}
