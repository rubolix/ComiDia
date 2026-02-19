package com.rubolix.comidia.data.local.dao

import androidx.room.*
import com.rubolix.comidia.data.local.entity.MealSlotEntity
import com.rubolix.comidia.data.local.entity.MealSlotWithRecipe
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {
    @Transaction
    @Query("SELECT * FROM meal_slots WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, mealType ASC")
    fun getMealSlotsForDateRange(startDate: String, endDate: String): Flow<List<MealSlotWithRecipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealSlot(mealSlot: MealSlotEntity)

    @Update
    suspend fun updateMealSlot(mealSlot: MealSlotEntity)

    @Delete
    suspend fun deleteMealSlot(mealSlot: MealSlotEntity)

    @Query("DELETE FROM meal_slots WHERE date = :date AND mealType = :mealType")
    suspend fun deleteMealSlot(date: String, mealType: String)

    @Query("UPDATE meal_slots SET recipeId = :recipeId WHERE id = :slotId")
    suspend fun assignRecipeToSlot(slotId: String, recipeId: String?)

    @Query("SELECT * FROM meal_slots WHERE date = :date")
    fun getMealSlotsForDate(date: String): Flow<List<MealSlotWithRecipe>>
}
