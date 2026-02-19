package com.rubolix.comidia.data.repository

import com.rubolix.comidia.data.local.dao.GoalDao
import com.rubolix.comidia.data.local.dao.MealPlanDao
import com.rubolix.comidia.data.local.dao.RecipeDao
import com.rubolix.comidia.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val recipeDao: RecipeDao
) {
    fun getAllRecipesWithTags(): Flow<List<RecipeWithTags>> = recipeDao.getAllRecipesWithTags()

    fun getAllRecipesWithTagsAndCategories(): Flow<List<RecipeWithTagsAndCategories>> =
        recipeDao.getAllRecipesWithTagsAndCategories()

    fun getRecipesByLatest(): Flow<List<RecipeWithTagsAndCategories>> = recipeDao.getRecipesByLatest()

    fun getRecipeFull(id: String): Flow<RecipeFull?> = recipeDao.getRecipeFull(id)

    fun searchRecipes(query: String): Flow<List<RecipeEntity>> = recipeDao.searchRecipes(query)

    fun getAllTags(): Flow<List<TagEntity>> = recipeDao.getAllTags()

    fun getAllCategories(): Flow<List<RecipeCategoryEntity>> = recipeDao.getAllCategories()

    suspend fun saveRecipe(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        tagIds: List<String>,
        categoryIds: List<String> = emptyList()
    ) {
        recipeDao.updateRecipeWithDetails(recipe, ingredients, tagIds, categoryIds)
    }

    suspend fun deleteRecipe(id: String) = recipeDao.deleteRecipeById(id)

    suspend fun archiveRecipe(id: String) = recipeDao.setArchived(id, true)

    suspend fun unarchiveRecipe(id: String) = recipeDao.setArchived(id, false)

    suspend fun copyRecipe(sourceId: String): String {
        val newId = UUID.randomUUID().toString()
        recipeDao.copyRecipe(sourceId, newId)
        return newId
    }

    suspend fun insertTag(tag: TagEntity) = recipeDao.insertTag(tag)

    suspend fun deleteTag(tag: TagEntity) = recipeDao.deleteTag(tag)

    suspend fun insertCategory(category: RecipeCategoryEntity) = recipeDao.insertCategory(category)

    suspend fun deleteCategory(category: RecipeCategoryEntity) = recipeDao.deleteCategory(category)
}

@Singleton
class MealPlanRepository @Inject constructor(
    private val mealPlanDao: MealPlanDao
) {
    fun getMealSlotsForWeek(startDate: String, endDate: String): Flow<List<MealSlotWithRecipes>> =
        mealPlanDao.getMealSlotsForDateRange(startDate, endDate)

    suspend fun addRecipeToSlot(date: String, mealType: String, recipeId: String) =
        mealPlanDao.addRecipeToSlot(date, mealType, recipeId)

    suspend fun removeRecipeFromSlot(date: String, mealType: String, recipeId: String) =
        mealPlanDao.removeRecipeFromSlot(date, mealType, recipeId)

    fun getWeeklyItems(weekStart: String): Flow<List<WeeklyItemEntity>> =
        mealPlanDao.getWeeklyItems(weekStart)

    suspend fun addWeeklyItem(item: WeeklyItemEntity) = mealPlanDao.insertWeeklyItem(item)

    suspend fun deleteWeeklyItem(item: WeeklyItemEntity) = mealPlanDao.deleteWeeklyItem(item)

    suspend fun toggleWeeklyItem(id: String, completed: Boolean) =
        mealPlanDao.setWeeklyItemCompleted(id, completed)
}

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao
) {
    fun getAllGoals(): Flow<List<MealPlanGoalEntity>> = goalDao.getAllGoals()

    fun getActiveGoals(): Flow<List<MealPlanGoalEntity>> = goalDao.getActiveGoals()

    suspend fun saveGoal(goal: MealPlanGoalEntity) = goalDao.insertGoal(goal)

    suspend fun deleteGoal(goal: MealPlanGoalEntity) = goalDao.deleteGoal(goal)

    suspend fun toggleGoal(id: String, active: Boolean) = goalDao.setGoalActive(id, active)
}
