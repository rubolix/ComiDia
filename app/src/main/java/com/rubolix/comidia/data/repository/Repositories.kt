package com.rubolix.comidia.data.repository

import com.rubolix.comidia.data.local.dao.MealPlanDao
import com.rubolix.comidia.data.local.dao.RecipeDao
import com.rubolix.comidia.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val recipeDao: RecipeDao
) {
    fun getAllRecipesWithTags(): Flow<List<RecipeWithTags>> = recipeDao.getAllRecipesWithTags()

    fun getRecipeFull(id: String): Flow<RecipeFull?> = recipeDao.getRecipeFull(id)

    fun searchRecipes(query: String): Flow<List<RecipeEntity>> = recipeDao.searchRecipes(query)

    fun getAllTags(): Flow<List<TagEntity>> = recipeDao.getAllTags()

    suspend fun saveRecipe(recipe: RecipeEntity, ingredients: List<IngredientEntity>, tagIds: List<String>) {
        recipeDao.updateRecipeWithDetails(recipe, ingredients, tagIds)
    }

    suspend fun deleteRecipe(id: String) = recipeDao.deleteRecipeById(id)

    suspend fun insertTag(tag: TagEntity) = recipeDao.insertTag(tag)

    suspend fun deleteTag(tag: TagEntity) = recipeDao.deleteTag(tag)
}

@Singleton
class MealPlanRepository @Inject constructor(
    private val mealPlanDao: MealPlanDao
) {
    fun getMealSlotsForWeek(startDate: String, endDate: String): Flow<List<MealSlotWithRecipe>> =
        mealPlanDao.getMealSlotsForDateRange(startDate, endDate)

    suspend fun insertMealSlot(slot: MealSlotEntity) = mealPlanDao.insertMealSlot(slot)

    suspend fun assignRecipe(slotId: String, recipeId: String?) = mealPlanDao.assignRecipeToSlot(slotId, recipeId)

    suspend fun deleteMealSlot(slot: MealSlotEntity) = mealPlanDao.deleteMealSlot(slot)
}
