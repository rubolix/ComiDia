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

    @Update
    suspend fun updateMealSlotRecipe(crossRef: MealSlotRecipeCrossRef)

    @Delete
    suspend fun deleteMealSlotRecipe(crossRef: MealSlotRecipeCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomEntry(entry: MealSlotCustomEntry)

    @Update
    suspend fun updateCustomEntry(entry: MealSlotCustomEntry)

    @Delete
    suspend fun deleteCustomEntry(entry: MealSlotCustomEntry)

    @Query("DELETE FROM meal_slot_recipes WHERE mealSlotId = :slotId")
    suspend fun clearMealSlotRecipes(slotId: String)

    @Transaction
    suspend fun addRecipeToSlot(date: String, mealType: String, recipeId: String, isLeftover: Boolean = false, generatesLeftovers: Boolean = false) {
        var slot = getMealSlotByDateAndType(date, mealType)
        if (slot == null) {
            slot = MealSlotEntity(date = date, mealType = mealType)
            insertMealSlot(slot)
        }
        val existingRefs = getRecipeCountForSlot(slot.id)
        val existingCustom = getCustomEntryCountForSlot(slot.id)
        insertMealSlotRecipe(MealSlotRecipeCrossRef(slot.id, recipeId, existingRefs + existingCustom, isLeftover, generatesLeftovers))
    }

    @Query("""
        SELECT r.*, ms.date as lastUsedDate, ms.mealType as lastUsedMealType FROM recipes r
        INNER JOIN meal_slot_recipes msr ON r.id = msr.recipeId
        INNER JOIN meal_slots ms ON msr.mealSlotId = ms.id
        WHERE ms.date BETWEEN :startDate AND :endDate
        AND r.isArchived = 0
        AND msr.generatesLeftovers = 1
        ORDER BY ms.date ASC, ms.mealType ASC
    """)
    fun getSourceLeftoversForRange(startDate: String, endDate: String): Flow<List<RecipeWithUsage>>

    // Custom data class for usage info
    data class RecipeWithUsage(
        @Embedded val recipe: RecipeEntity,
        val lastUsedDate: String,
        val lastUsedMealType: String
    )

    @Transaction
    suspend fun addCustomEntryToSlot(date: String, mealType: String, title: String, type: String, isLeftover: Boolean = false, generatesLeftovers: Boolean = false) {
        var slot = getMealSlotByDateAndType(date, mealType)
        if (slot == null) {
            slot = MealSlotEntity(date = date, mealType = mealType)
            insertMealSlot(slot)
        }
        val existingRefs = getRecipeCountForSlot(slot.id)
        val existingCustom = getCustomEntryCountForSlot(slot.id)
        insertCustomEntry(MealSlotCustomEntry(mealSlotId = slot.id, title = title, type = type, sortOrder = existingRefs + existingCustom, isLeftover = isLeftover, generatesLeftovers = generatesLeftovers))
    }

    @Query("SELECT COUNT(*) FROM meal_slot_custom_entries WHERE mealSlotId = :slotId")
    suspend fun getCustomEntryCountForSlot(slotId: String): Int

    @Query("SELECT COUNT(*) FROM meal_slot_recipes WHERE mealSlotId = :slotId")
    suspend fun getRecipeCountForSlot(slotId: String): Int

    @Transaction
    suspend fun removeRecipeFromSlot(date: String, mealType: String, recipeId: String) {
        val slot = getMealSlotByDateAndType(date, mealType) ?: return
        deleteMealSlotRecipe(MealSlotRecipeCrossRef(slot.id, recipeId))
        if (getRecipeCountForSlot(slot.id) == 0 && getCustomEntryCountForSlot(slot.id) == 0) {
            deleteMealSlot(slot)
        }
    }

    @Transaction
    suspend fun removeCustomEntryFromSlot(id: String) {
        val entry = getCustomEntryById(id) ?: return
        deleteCustomEntry(entry)
        val slotId = entry.mealSlotId
        if (getRecipeCountForSlot(slotId) == 0 && getCustomEntryCountForSlot(slotId) == 0) {
            deleteMealSlotById(slotId)
        }
    }

    @Query("SELECT * FROM meal_slot_custom_entries WHERE id = :id")
    suspend fun getCustomEntryById(id: String): MealSlotCustomEntry?

    @Query("DELETE FROM meal_slots WHERE id = :id")
    suspend fun deleteMealSlotById(id: String)

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

    // Week Metadata
    @Query("SELECT * FROM week_metadata WHERE weekStartDate = :weekStart")
    fun getWeekMetadata(weekStart: String): Flow<WeekMetadata?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeekMetadata(metadata: WeekMetadata)
}
