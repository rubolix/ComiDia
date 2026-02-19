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

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: String)

    @Query("UPDATE recipes SET isArchived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long = System.currentTimeMillis())

    // Ingredients
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: IngredientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsForRecipe(recipeId: String)

    // Tags
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: RecipeCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: RecipeCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecipeCategoryCrossRef(crossRef: RecipeCategoryCrossRef)

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
        tagIds.forEach { tagId ->
            insertRecipeTagCrossRef(RecipeTagCrossRef(recipe.id, tagId))
        }
        deleteAllCategoriesForRecipe(recipe.id)
        categoryIds.forEach { catId ->
            insertRecipeCategoryCrossRef(RecipeCategoryCrossRef(recipe.id, catId))
        }
    }

    // Copy a recipe
    @Transaction
    suspend fun copyRecipe(sourceId: String, newId: String) {
        val source = getRecipeById(sourceId) ?: return
        insertRecipe(source.copy(id = newId, name = "${source.name} (Copy)", updatedAt = System.currentTimeMillis()))
    }
}
