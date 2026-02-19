package com.rubolix.comidia.data.local.dao

import androidx.room.*
import com.rubolix.comidia.data.local.entity.MealPlanGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM meal_plan_goals ORDER BY description ASC")
    fun getAllGoals(): Flow<List<MealPlanGoalEntity>>

    @Query("SELECT * FROM meal_plan_goals WHERE isActive = 1")
    fun getActiveGoals(): Flow<List<MealPlanGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: MealPlanGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: MealPlanGoalEntity)

    @Query("UPDATE meal_plan_goals SET isActive = :active WHERE id = :id")
    suspend fun setGoalActive(id: String, active: Boolean)
}
