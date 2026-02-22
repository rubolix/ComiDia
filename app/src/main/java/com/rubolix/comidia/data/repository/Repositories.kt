package com.rubolix.comidia.data.repository

import com.rubolix.comidia.data.local.dao.GoalDao
import com.rubolix.comidia.data.local.dao.MealPlanDao
import com.rubolix.comidia.data.local.dao.RecipeDao
import com.rubolix.comidia.data.local.dao.SettingsDao
import com.rubolix.comidia.data.local.dao.StapleDao
import com.rubolix.comidia.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    fun getMostFrequentTags(): Flow<List<TagEntity>> = recipeDao.getMostFrequentTags()

    fun getAllCategories(): Flow<List<RecipeCategoryEntity>> = recipeDao.getAllCategories()

    fun getRootCategories(): Flow<List<RecipeCategoryEntity>> = recipeDao.getRootCategories()

    fun getSubcategories(parentId: String): Flow<List<RecipeCategoryEntity>> = recipeDao.getSubcategories(parentId)

    suspend fun insertCategory(category: RecipeCategoryEntity) = recipeDao.insertCategory(category)

    suspend fun updateCategory(category: RecipeCategoryEntity) = recipeDao.updateCategory(category)

    suspend fun deleteCategory(category: RecipeCategoryEntity) = recipeDao.deleteCategory(category)

    suspend fun batchAddRecipesToCategory(recipeIds: List<String>, categoryId: String) =
        recipeDao.batchAddRecipesToCategory(recipeIds, categoryId)

    suspend fun batchRemoveRecipesFromCategory(recipeIds: List<String>, categoryId: String) =
        recipeDao.batchRemoveRecipesFromCategory(recipeIds, categoryId)

    fun getSmartRuleForCategory(categoryId: String): Flow<CategorySmartRuleEntity?> =
        recipeDao.getSmartRuleForCategory(categoryId)

    suspend fun saveSmartRule(rule: CategorySmartRuleEntity) =
        recipeDao.insertSmartRule(rule)

    suspend fun deleteSmartRuleForCategory(categoryId: String) =
        recipeDao.deleteSmartRuleForCategory(categoryId)

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

    suspend fun saveTag(tag: TagEntity) = recipeDao.insertTag(tag)

    suspend fun deleteTag(tag: TagEntity) = recipeDao.deleteTag(tag)

    fun getIngredientPreferences(weekStart: String): Flow<List<UserIngredientPreference>> =
        recipeDao.getIngredientPreferences(weekStart)

    suspend fun saveIngredientPreference(pref: UserIngredientPreference) =
        recipeDao.insertIngredientPreference(pref)

    suspend fun clearIngredientPreference(weekStart: String, name: String) =
        recipeDao.deleteIngredientPreference(weekStart, name)

    suspend fun updateFavoritesTag() {
        val topRated = recipeDao.getRecipesByMinRating(4f)
        if (topRated.isEmpty()) return
        val threeMonthsAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
        val usageCounts = recipeDao.getRecipeUsageCountsSince(threeMonthsAgo)
            .associate { it.recipeId to it.count }
        val candidates = topRated.map { it to (usageCounts[it.id] ?: 0) }
            .sortedByDescending { it.second }
        val countToTake = (candidates.size * 0.25).toInt().coerceAtLeast(1)
        val favoriteIds = candidates.take(countToTake).map { it.first.id }.toSet()
        val favoritesTag = recipeDao.getTagByName("Favorites") ?: return
        recipeDao.updateTagLinksForRecipes(favoritesTag.id, favoriteIds)
    }
}

@Singleton
class StapleRepository @Inject constructor(
    private val stapleDao: StapleDao
) {
    fun getAllStaples(): Flow<List<StapleEntity>> = stapleDao.getAllStaples()
    fun getAllStaplesIncludingRemoved(): Flow<List<StapleEntity>> = stapleDao.getAllStaplesIncludingRemoved()
    suspend fun saveStaple(staple: StapleEntity) = stapleDao.insertStaple(staple)
    suspend fun updateStaple(staple: StapleEntity) = stapleDao.updateStaple(staple)
    suspend fun deleteStaple(staple: StapleEntity) = stapleDao.deleteStaple(staple)
    suspend fun markAsRemoved(id: String) = stapleDao.markAsRemoved(id)
    suspend fun restoreStaple(id: String) = stapleDao.restoreStaple(id)
}

@Singleton
class MealPlanRepository @Inject constructor(
    private val mealPlanDao: MealPlanDao
) {
    fun getMealSlotsForWeek(start: String, end: String): Flow<List<MealSlotWithRecipes>> =
        mealPlanDao.getMealSlotsForDateRange(start, end)

    fun getSourceLeftoversForRange(start: String, end: String): Flow<List<MealPlanDao.RecipeWithUsage>> =
        mealPlanDao.getSourceLeftoversForRange(start, end)

    suspend fun addRecipeToSlot(date: String, type: String, recipeId: String, isLeftover: Boolean, generates: Boolean) =
        mealPlanDao.addRecipeToSlot(date, type, recipeId, isLeftover, generates)

    suspend fun addCustomEntryToSlot(date: String, type: String, title: String, entryType: String, isLeftover: Boolean, generates: Boolean) =
        mealPlanDao.addCustomEntryToSlot(date, type, title, entryType, isLeftover, generates)

    suspend fun removeRecipeFromSlot(date: String, type: String, recipeId: String) =
        mealPlanDao.removeRecipeFromSlot(date, type, recipeId)

    suspend fun removeCustomEntryFromSlot(entryId: String) =
        mealPlanDao.removeCustomEntryFromSlot(entryId)

    suspend fun updateMealSlotRecipe(crossRef: MealSlotRecipeCrossRef) =
        mealPlanDao.updateMealSlotRecipe(crossRef)

    suspend fun updateCustomEntry(entry: MealSlotCustomEntry) =
        mealPlanDao.updateCustomEntry(entry)

    fun getWeeklyItems(weekStart: String): Flow<List<WeeklyItemEntity>> =
        mealPlanDao.getWeeklyItems(weekStart)

    suspend fun addWeeklyItem(item: WeeklyItemEntity) =
        mealPlanDao.insertWeeklyItem(item)

    suspend fun deleteWeeklyItem(item: WeeklyItemEntity) =
        mealPlanDao.deleteWeeklyItem(item)

    suspend fun toggleWeeklyItem(id: String, completed: Boolean) =
        mealPlanDao.setWeeklyItemCompleted(id, completed)

    fun getDailyTodos(start: String, end: String): Flow<List<DailyTodoEntity>> =
        mealPlanDao.getDailyTodosForRange(start, end)

    suspend fun addDailyTodo(todo: DailyTodoEntity) =
        mealPlanDao.insertDailyTodo(todo)

    suspend fun deleteDailyTodo(todo: DailyTodoEntity) =
        mealPlanDao.deleteDailyTodo(todo)

    suspend fun toggleDailyTodo(id: String, completed: Boolean) =
        mealPlanDao.setDailyTodoCompleted(id, completed)

    fun getWeekMetadata(weekStart: String): Flow<WeekMetadata?> =
        mealPlanDao.getWeekMetadata(weekStart)

    suspend fun saveWeekMetadata(metadata: WeekMetadata) =
        mealPlanDao.insertWeekMetadata(metadata)
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

@Singleton
class AppSettingsRepository @Inject constructor(
    private val settingsDao: com.rubolix.comidia.data.local.dao.SettingsDao
) {
    fun getFirstDayOfWeek(): Flow<String> = settingsDao.getSetting(AppSettingsEntity.FIRST_DAY_OF_WEEK)
        .map { it?.value ?: "monday" }

    suspend fun setFirstDayOfWeek(day: String) {
        settingsDao.setSetting(AppSettingsEntity(AppSettingsEntity.FIRST_DAY_OF_WEEK, day))
    }

    fun getDefaultMealTypes(): Flow<Set<String>> = settingsDao.getSetting(AppSettingsEntity.DEFAULT_MEAL_TYPES)
        .map { it?.value?.split(",")?.toSet() ?: setOf("dinner") }

    suspend fun setDefaultMealTypes(types: Set<String>) {
        settingsDao.setSetting(AppSettingsEntity(AppSettingsEntity.DEFAULT_MEAL_TYPES, types.joinToString(",")))
    }
}
