package com.rubolix.comidia.data.local.dao

import androidx.room.*
import com.rubolix.comidia.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {
    @Transaction
    @Query("SELECT * FROM meal_slots WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, mealType ASC")
    fun getMealSlotsForDateRange(startDate: String, endDate: String): Flow<List<MealSlotWithRecipes>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealSlot(mealSlot: MealSlotEntity)

    @Delete
    suspend fun deleteMealSlot(mealSlot: MealSlotEntity)

    @Query("DELETE FROM meal_slots WHERE date = :date AND mealType = :mealType")
    suspend fun deleteMealSlotByDateAndType(date: String, mealType: String)

    @Query("SELECT * FROM meal_slots WHERE date = :date AND mealType = :mealType")
    suspend fun getMealSlotByDateAndType(date: String, mealType: String): MealSlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealSlotRecipe(crossRef: MealSlotRecipeCrossRef)

    @Delete
    suspend fun deleteMealSlotRecipe(crossRef: MealSlotRecipeCrossRef)

    @Query("DELETE FROM meal_slot_recipes WHERE mealSlotId = :slotId")
    suspend fun clearMealSlotRecipes(slotId: String)

    @Transaction
    suspend fun addRecipeToSlot(date: String, mealType: String, recipeId: String) {
        var slot = getMealSlotByDateAndType(date, mealType)
        if (slot == null) {
            slot = MealSlotEntity(date = date, mealType = mealType)
            insertMealSlot(slot)
        }
        val existing = getRecipeCountForSlot(slot.id)
        insertMealSlotRecipe(MealSlotRecipeCrossRef(slot.id, recipeId, existing))
    }

    @Query("SELECT COUNT(*) FROM meal_slot_recipes WHERE mealSlotId = :slotId")
    suspend fun getRecipeCountForSlot(slotId: String): Int

    @Transaction
    suspend fun removeRecipeFromSlot(date: String, mealType: String, recipeId: String) {
        val slot = getMealSlotByDateAndType(date, mealType) ?: return
        deleteMealSlotRecipe(MealSlotRecipeCrossRef(slot.id, recipeId))
        if (getRecipeCountForSlot(slot.id) == 0) {
            deleteMealSlot(slot)
        }
    }

    // Weekly items
    @Query("SELECT * FROM weekly_items WHERE weekStartDate = :weekStart ORDER BY id ASC")
    fun getWeeklyItems(weekStart: String): Flow<List<WeeklyItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyItem(item: WeeklyItemEntity)

    @Delete
    suspend fun deleteWeeklyItem(item: WeeklyItemEntity)

    @Query("UPDATE weekly_items SET isCompleted = :completed WHERE id = :id")
    suspend fun setWeeklyItemCompleted(id: String, completed: Boolean)

    // Daily todos
    @Query("SELECT * FROM daily_todos WHERE date = :date ORDER BY id ASC")
    fun getDailyTodos(date: String): Flow<List<DailyTodoEntity>>

    @Query("SELECT * FROM daily_todos WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, id ASC")
    fun getDailyTodosForRange(startDate: String, endDate: String): Flow<List<DailyTodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyTodo(todo: DailyTodoEntity)

    @Delete
    suspend fun deleteDailyTodo(todo: DailyTodoEntity)

    @Query("UPDATE daily_todos SET isCompleted = :completed WHERE id = :id")
    suspend fun setDailyTodoCompleted(id: String, completed: Boolean)
}
