package com.rubolix.comidia.data.local.dao

import androidx.room.*
import com.rubolix.comidia.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Transaction
    @Query("SELECT * FROM recipes WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllRecipesWithTags(): Flow<List<RecipeWithTags>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllRecipesWithTagsAndCategories(): Flow<List<RecipeWithTagsAndCategories>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getRecipesByLatest(): Flow<List<RecipeWithTagsAndCategories>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE isArchived = 1 ORDER BY name ASC")
    fun getArchivedRecipes(): Flow<List<RecipeWithTags>>

    @Query("SELECT * FROM ingredients WHERE recipeId IN (:recipeIds)")
    suspend fun getIngredientsByRecipeIds(recipeIds: List<String>): List<IngredientEntity>

    @Query("SELECT * FROM recipes WHERE id IN (:ids)")
    suspend fun getRecipesByIds(ids: List<String>): List<RecipeEntity>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun getRecipeFull(id: String): Flow<RecipeFull?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeFullSync(id: String): RecipeFull?

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeById(id: String): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE name LIKE '%' || :query || '%' AND isArchived = 0 ORDER BY name ASC")
    fun searchRecipes(query: String): Flow<List<RecipeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: String)

    @Query("UPDATE recipes SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: String, isArchived: Boolean)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsForRecipe(recipeId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("""
        SELECT t.* FROM tags t
        LEFT JOIN recipe_tag_cross_ref ref ON t.id = ref.tagId
        GROUP BY t.id
        ORDER BY COUNT(ref.recipeId) DESC, t.name ASC
    """)
    fun getMostFrequentTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecipeTagCrossRef(crossRef: RecipeTagCrossRef)

    @Delete
    suspend fun deleteRecipeTagCrossRef(crossRef: RecipeTagCrossRef)

    @Query("DELETE FROM recipe_tag_cross_ref WHERE recipeId = :recipeId")
    suspend fun deleteAllTagsForRecipe(recipeId: String)

    // Categories
    @Query("SELECT * FROM recipe_categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<RecipeCategoryEntity>>

    @Query("SELECT * FROM recipe_categories WHERE parentId IS NULL ORDER BY sortOrder ASC")
    fun getRootCategories(): Flow<List<RecipeCategoryEntity>>

    @Query("SELECT * FROM recipe_categories WHERE parentId = :parentId ORDER BY sortOrder ASC")
    fun getSubcategories(parentId: String): Flow<List<RecipeCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: RecipeCategoryEntity)

    @Update
    suspend fun updateCategory(category: RecipeCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: RecipeCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecipeCategoryCrossRef(crossRef: RecipeCategoryCrossRef)

    @Query("DELETE FROM recipe_category_cross_ref WHERE recipeId = :recipeId AND categoryId = :categoryId")
    suspend fun removeRecipeFromCategory(recipeId: String, categoryId: String)

    @Query("DELETE FROM recipe_category_cross_ref WHERE recipeId = :recipeId")
    suspend fun deleteAllCategoriesForRecipe(recipeId: String)

    @Transaction
    suspend fun updateRecipeWithDetails(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        tagIds: List<String>,
        categoryIds: List<String> = emptyList()
    ) {
        insertRecipe(recipe)
        deleteIngredientsForRecipe(recipe.id)
        insertIngredients(ingredients)
        
        deleteAllTagsForRecipe(recipe.id)
        tagIds.forEach { tid -> insertRecipeTagCrossRef(RecipeTagCrossRef(recipe.id, tid)) }
        
        deleteAllCategoriesForRecipe(recipe.id)
        
        // Manual categories
        categoryIds.forEach { cid -> insertRecipeCategoryCrossRef(RecipeCategoryCrossRef(recipe.id, cid)) }
        
        // Auto-categorization from tags
        val autoCatIds = getCategoryIdsForTags(tagIds)
        val existingCatIds = getAllCategoryIds()
        autoCatIds.filter { it in existingCatIds }.forEach { cid -> 
            insertRecipeCategoryCrossRef(RecipeCategoryCrossRef(recipe.id, cid)) 
        }
    }

    @Query("SELECT id FROM recipe_categories")
    suspend fun getAllCategoryIds(): List<String>

    @Query("SELECT categoryId FROM tags WHERE id IN (:tagIds) AND categoryId IS NOT NULL")
    suspend fun getCategoryIdsForTags(tagIds: List<String>): List<String>

    // Smart Rules
    @Query("SELECT * FROM category_smart_rules WHERE categoryId = :categoryId LIMIT 1")
    fun getSmartRuleForCategory(categoryId: String): Flow<CategorySmartRuleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmartRule(rule: CategorySmartRuleEntity)

    @Update
    suspend fun updateSmartRule(rule: CategorySmartRuleEntity)

    @Query("DELETE FROM category_smart_rules WHERE categoryId = :categoryId")
    suspend fun deleteSmartRuleForCategory(categoryId: String)

    @Transaction
    suspend fun batchAddRecipesToCategory(recipeIds: List<String>, categoryId: String) {
        recipeIds.forEach { rid -> insertRecipeCategoryCrossRef(RecipeCategoryCrossRef(rid, categoryId)) }
    }

    @Transaction
    suspend fun batchRemoveRecipesFromCategory(recipeIds: List<String>, categoryId: String) {
        recipeIds.forEach { rid -> removeRecipeFromCategory(rid, categoryId) }
    }

    // Copy a recipe
    @Transaction
    suspend fun copyRecipe(sourceId: String, newId: String) {
        val source = getRecipeById(sourceId) ?: return
        insertRecipe(source.copy(id = newId, name = "${source.name} (Copy)", updatedAt = System.currentTimeMillis()))
    }

    // Ingredient Preferences
    @Query("SELECT * FROM user_ingredient_preferences WHERE weekStartDate = :weekStart")
    fun getIngredientPreferences(weekStart: String): Flow<List<UserIngredientPreference>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredientPreference(pref: UserIngredientPreference)

    @Query("DELETE FROM user_ingredient_preferences WHERE weekStartDate = :weekStart AND ingredientName = :name")
    suspend fun deleteIngredientPreference(weekStart: String, name: String)

    @Query("SELECT * FROM recipes WHERE rating >= :minRating AND isArchived = 0")
    suspend fun getRecipesByMinRating(minRating: Float): List<RecipeEntity>

    @Query("""
        SELECT recipeId, COUNT(*) as count FROM meal_slot_recipes msr
        INNER JOIN meal_slots ms ON msr.mealSlotId = ms.id
        WHERE ms.date >= :sinceDate
        GROUP BY recipeId
    """)
    suspend fun getRecipeUsageCountsSince(sinceDate: Long): List<RecipeUsageCount>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Transaction
    suspend fun updateTagLinksForRecipes(tagId: String, recipeIds: Set<String>) {
        recipeIds.forEach { rid ->
            insertRecipeTagCrossRef(RecipeTagCrossRef(rid, tagId))
        }
    }

    data class RecipeUsageCount(val recipeId: String, val count: Int)
}
