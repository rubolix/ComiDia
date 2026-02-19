package com.rubolix.comidia.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rubolix.comidia.data.local.dao.RecipeDao
import com.rubolix.comidia.data.local.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeDaoTest {

    private lateinit var db: ComiDiaDatabase
    private lateinit var recipeDao: RecipeDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ComiDiaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        recipeDao = db.recipeDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveRecipe() = runTest {
        val recipe = RecipeEntity(
            id = "test-1",
            name = "Test Recipe",
            instructions = "Mix and cook",
            servings = 4,
            prepTimeMinutes = 10,
            cookTimeMinutes = 20,
            rating = 4.5f,
            notes = "Great recipe"
        )
        recipeDao.insertRecipe(recipe)

        val result = recipeDao.getRecipeById("test-1")
        assertNotNull(result)
        assertEquals("Test Recipe", result!!.name)
        assertEquals(4.5f, result.rating, 0.001f)
        assertEquals("Great recipe", result.notes)
        assertEquals(30, result.totalTimeMinutes)
    }

    @Test
    fun insertAndRetrieveRecipeFull() = runTest {
        val recipe = RecipeEntity(id = "r1", name = "Full Recipe")
        recipeDao.insertRecipe(recipe)

        val tag = TagEntity(id = "t1", name = "Vegetarian")
        recipeDao.insertTag(tag)
        recipeDao.insertRecipeTagCrossRef(RecipeTagCrossRef("r1", "t1"))

        val ingredient = IngredientEntity(
            id = "i1", recipeId = "r1", name = "Tomato",
            quantity = "2", unit = "cups", category = "Produce"
        )
        recipeDao.insertIngredient(ingredient)

        val full = recipeDao.getRecipeFull("r1").first()
        assertNotNull(full)
        assertEquals("Full Recipe", full!!.recipe.name)
        assertEquals(1, full.tags.size)
        assertEquals("Vegetarian", full.tags[0].name)
        assertEquals(1, full.ingredients.size)
        assertEquals("Tomato", full.ingredients[0].name)
        assertEquals("Produce", full.ingredients[0].category)
    }

    @Test
    fun getAllRecipesWithTags_excludesArchived() = runTest {
        recipeDao.insertRecipe(RecipeEntity(id = "r1", name = "Active Recipe"))
        recipeDao.insertRecipe(RecipeEntity(id = "r2", name = "Archived Recipe", isArchived = true))

        val recipes = recipeDao.getAllRecipesWithTags().first()
        assertEquals(1, recipes.size)
        assertEquals("Active Recipe", recipes[0].recipe.name)
    }

    @Test
    fun archiveAndUnarchiveRecipe() = runTest {
        recipeDao.insertRecipe(RecipeEntity(id = "r1", name = "Recipe"))

        recipeDao.setArchived("r1", true)
        var recipe = recipeDao.getRecipeById("r1")
        assertTrue(recipe!!.isArchived)

        recipeDao.setArchived("r1", false)
        recipe = recipeDao.getRecipeById("r1")
        assertFalse(recipe!!.isArchived)
    }

    @Test
    fun deleteRecipe() = runTest {
        recipeDao.insertRecipe(RecipeEntity(id = "r1", name = "To Delete"))
        assertNotNull(recipeDao.getRecipeById("r1"))

        recipeDao.deleteRecipeById("r1")
        assertNull(recipeDao.getRecipeById("r1"))
    }

    @Test
    fun updateRecipeWithDetails_replacesOldData() = runTest {
        // Initial insert
        val recipe = RecipeEntity(id = "r1", name = "Original")
        val ingredients = listOf(
            IngredientEntity(id = "i1", recipeId = "r1", name = "Salt")
        )
        val tag = TagEntity(id = "t1", name = "Quick")
        recipeDao.insertTag(tag)
        recipeDao.updateRecipeWithDetails(recipe, ingredients, listOf("t1"))

        // Update with new data
        val updated = recipe.copy(name = "Updated")
        val newIngredients = listOf(
            IngredientEntity(id = "i2", recipeId = "r1", name = "Pepper"),
            IngredientEntity(id = "i3", recipeId = "r1", name = "Garlic")
        )
        recipeDao.updateRecipeWithDetails(updated, newIngredients, emptyList())

        val full = recipeDao.getRecipeFull("r1").first()
        assertNotNull(full)
        assertEquals("Updated", full!!.recipe.name)
        assertEquals(2, full.ingredients.size)
        assertEquals(0, full.tags.size) // tags cleared
    }

    @Test
    fun searchRecipes_findsMatches() = runTest {
        recipeDao.insertRecipe(RecipeEntity(id = "r1", name = "Chicken Tacos"))
        recipeDao.insertRecipe(RecipeEntity(id = "r2", name = "Beef Stew"))
        recipeDao.insertRecipe(RecipeEntity(id = "r3", name = "Grilled Chicken"))

        val results = recipeDao.searchRecipes("chicken").first()
        assertEquals(2, results.size)
    }

    @Test
    fun getRecipesByLatest_orderedByUpdatedAt() = runTest {
        recipeDao.insertRecipe(RecipeEntity(id = "r1", name = "Old", updatedAt = 1000))
        recipeDao.insertRecipe(RecipeEntity(id = "r2", name = "New", updatedAt = 2000))

        val recipes = recipeDao.getRecipesByLatest().first()
        assertEquals("New", recipes[0].recipe.name)
        assertEquals("Old", recipes[1].recipe.name)
    }

    @Test
    fun getAllTags_returnsSorted() = runTest {
        recipeDao.insertTag(TagEntity(id = "t1", name = "Zebra"))
        recipeDao.insertTag(TagEntity(id = "t2", name = "Apple"))

        val tags = recipeDao.getAllTags().first()
        assertEquals("Apple", tags[0].name)
        assertEquals("Zebra", tags[1].name)
    }

    @Test
    fun getIngredientsByRecipeIds_batchQuery() = runTest {
        recipeDao.insertRecipe(RecipeEntity(id = "r1", name = "Recipe 1"))
        recipeDao.insertRecipe(RecipeEntity(id = "r2", name = "Recipe 2"))

        recipeDao.insertIngredients(listOf(
            IngredientEntity(id = "i1", recipeId = "r1", name = "Salt"),
            IngredientEntity(id = "i2", recipeId = "r1", name = "Pepper"),
            IngredientEntity(id = "i3", recipeId = "r2", name = "Garlic"),
            IngredientEntity(id = "i4", recipeId = "r3", name = "Not included")
        ))

        val results = recipeDao.getIngredientsByRecipeIds(listOf("r1", "r2"))
        assertEquals(3, results.size)
    }

    @Test
    fun copyRecipe_createsNewWithCopySuffix() = runTest {
        recipeDao.insertRecipe(RecipeEntity(id = "r1", name = "Original Recipe"))

        recipeDao.copyRecipe("r1", "r2")

        val copy = recipeDao.getRecipeById("r2")
        assertNotNull(copy)
        assertEquals("Original Recipe (Copy)", copy!!.name)
    }

    @Test
    fun recipeWithSourceUrlAndRating() = runTest {
        val recipe = RecipeEntity(
            id = "r1",
            name = "Web Recipe",
            sourceUrl = "https://example.com/recipe",
            rating = 3.5f,
            notes = "Found this online"
        )
        recipeDao.insertRecipe(recipe)

        val result = recipeDao.getRecipeById("r1")
        assertEquals("https://example.com/recipe", result!!.sourceUrl)
        assertEquals(3.5f, result.rating, 0.001f)
        assertEquals("Found this online", result.notes)
    }
}
