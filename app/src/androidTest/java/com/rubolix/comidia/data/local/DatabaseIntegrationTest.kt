package com.rubolix.comidia.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rubolix.comidia.data.local.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests that verify end-to-end data flows
 * across multiple DAOs and entities.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {

    private lateinit var db: ComiDiaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ComiDiaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun fullMealPlanWorkflow() = runTest {
        val recipeDao = db.recipeDao()
        val mealPlanDao = db.mealPlanDao()

        // Create recipes with tags
        val fishTag = TagEntity(id = "t-fish", name = "Fish")
        val vegTag = TagEntity(id = "t-veg", name = "Vegetarian")
        recipeDao.insertTag(fishTag)
        recipeDao.insertTag(vegTag)

        val salmon = RecipeEntity(id = "r-salmon", name = "Baked Salmon", rating = 4.5f)
        val pasta = RecipeEntity(id = "r-pasta", name = "Veggie Pasta", rating = 4.0f)
        recipeDao.insertRecipe(salmon)
        recipeDao.insertRecipe(pasta)

        recipeDao.insertRecipeTagCrossRef(RecipeTagCrossRef("r-salmon", "t-fish"))
        recipeDao.insertRecipeTagCrossRef(RecipeTagCrossRef("r-pasta", "t-veg"))

        // Add ingredients
        recipeDao.insertIngredients(listOf(
            IngredientEntity(recipeId = "r-salmon", name = "Salmon fillet", quantity = "4", unit = "pieces", category = "Protein"),
            IngredientEntity(recipeId = "r-salmon", name = "Lemon", quantity = "2", unit = "", category = "Produce"),
            IngredientEntity(recipeId = "r-pasta", name = "Penne", quantity = "1", unit = "lb", category = "Pantry"),
            IngredientEntity(recipeId = "r-pasta", name = "Mushrooms", quantity = "8", unit = "oz", category = "Produce")
        ))

        // Plan a week
        mealPlanDao.addRecipeToSlot("2024-01-15", "dinner", "r-salmon")
        mealPlanDao.addRecipeToSlot("2024-01-16", "dinner", "r-pasta")

        // Verify meal slots
        val slots = mealPlanDao.getMealSlotsForDateRange("2024-01-15", "2024-01-21").first()
        assertEquals(2, slots.size)

        // Verify we can get ingredients for planned recipes
        val recipeIds = slots.flatMap { it.recipes.map { r -> r.id } }
        val ingredients = recipeDao.getIngredientsByRecipeIds(recipeIds)
        assertEquals(4, ingredients.size)

        // Verify recipe details are accessible
        val salmonFull = recipeDao.getRecipeFull("r-salmon").first()
        assertNotNull(salmonFull)
        assertEquals(4.5f, salmonFull!!.recipe.rating, 0.001f)
        assertEquals(1, salmonFull.tags.size)
        assertEquals("Fish", salmonFull.tags[0].name)
        assertEquals(2, salmonFull.ingredients.size)
    }

    @Test
    fun settingsWorkflow() = runTest {
        val settingsDao = db.settingsDao()

        // Set first day of week
        settingsDao.setSetting(AppSettingsEntity(AppSettingsEntity.FIRST_DAY_OF_WEEK, "sunday"))

        // Set default meal types
        settingsDao.setSetting(AppSettingsEntity(AppSettingsEntity.DEFAULT_MEAL_TYPES, "breakfast,dinner"))

        // Verify
        val firstDay = settingsDao.getSetting(AppSettingsEntity.FIRST_DAY_OF_WEEK).first()
        assertEquals("sunday", firstDay!!.value)

        val mealTypes = settingsDao.getSetting(AppSettingsEntity.DEFAULT_MEAL_TYPES).first()
        val types = mealTypes!!.value.split(",").toSet()
        assertTrue(types.contains("breakfast"))
        assertTrue(types.contains("dinner"))
        assertFalse(types.contains("lunch"))
    }

    @Test
    fun goalTrackingWorkflow() = runTest {
        val goalDao = db.goalDao()
        val recipeDao = db.recipeDao()
        val mealPlanDao = db.mealPlanDao()

        // Set up goals
        goalDao.insertGoal(MealPlanGoalEntity(
            id = "g1", description = "Fish", tagId = "t-fish",
            goalType = "gte", targetCount = 1, period = "week"
        ))
        goalDao.insertGoal(MealPlanGoalEntity(
            id = "g2", description = "Pasta", tagId = "t-pasta",
            goalType = "lte", targetCount = 2, period = "week"
        ))

        // Create recipes
        val fishTag = TagEntity(id = "t-fish", name = "Fish")
        val pastaTag = TagEntity(id = "t-pasta", name = "Pasta")
        recipeDao.insertTag(fishTag)
        recipeDao.insertTag(pastaTag)

        recipeDao.insertRecipe(RecipeEntity(id = "r1", name = "Salmon"))
        recipeDao.insertRecipeTagCrossRef(RecipeTagCrossRef("r1", "t-fish"))

        // Plan the week
        mealPlanDao.addRecipeToSlot("2024-01-15", "dinner", "r1")

        // Verify goals
        val goals = goalDao.getActiveGoals().first()
        assertEquals(2, goals.size)

        // Verify recipe tags for goal evaluation
        val recipesWithTags = recipeDao.getAllRecipesWithTags().first()
        val fishRecipes = recipesWithTags.filter { rwt ->
            rwt.tags.any { it.id == "t-fish" }
        }
        assertEquals(1, fishRecipes.size)
    }

    @Test
    fun recipeArchivingWorkflow() = runTest {
        val recipeDao = db.recipeDao()

        recipeDao.insertRecipe(RecipeEntity(id = "r1", name = "Recipe A"))
        recipeDao.insertRecipe(RecipeEntity(id = "r2", name = "Recipe B"))
        recipeDao.insertRecipe(RecipeEntity(id = "r3", name = "Recipe C"))

        // Archive one
        recipeDao.setArchived("r2", true)

        // Active recipes should be 2
        val active = recipeDao.getAllRecipesWithTags().first()
        assertEquals(2, active.size)

        // Archived should be 1
        val archived = recipeDao.getArchivedRecipes().first()
        assertEquals(1, archived.size)
        assertEquals("Recipe B", archived[0].recipe.name)

        // Unarchive
        recipeDao.setArchived("r2", false)
        val allActive = recipeDao.getAllRecipesWithTags().first()
        assertEquals(3, allActive.size)
    }

    @Test
    fun weeklyItemsAndTodos_workflow() = runTest {
        val mealPlanDao = db.mealPlanDao()

        // Add weekly items
        mealPlanDao.insertWeeklyItem(WeeklyItemEntity(id = "w1", weekStartDate = "2024-01-15", text = "Buy fruit"))
        mealPlanDao.insertWeeklyItem(WeeklyItemEntity(id = "w2", weekStartDate = "2024-01-15", text = "Clean fridge"))

        // Add daily todos
        mealPlanDao.insertDailyTodo(DailyTodoEntity(id = "d1", date = "2024-01-15", text = "Defrost chicken"))
        mealPlanDao.insertDailyTodo(DailyTodoEntity(id = "d2", date = "2024-01-16", text = "Marinate pork"))

        // Complete some
        mealPlanDao.setWeeklyItemCompleted("w1", true)
        mealPlanDao.setDailyTodoCompleted("d1", true)

        // Verify
        val weeklyItems = mealPlanDao.getWeeklyItems("2024-01-15").first()
        assertEquals(2, weeklyItems.size)
        assertTrue(weeklyItems.find { it.id == "w1" }!!.isCompleted)
        assertFalse(weeklyItems.find { it.id == "w2" }!!.isCompleted)

        val todos = mealPlanDao.getDailyTodosForRange("2024-01-15", "2024-01-21").first()
        assertEquals(2, todos.size)
        assertTrue(todos.find { it.id == "d1" }!!.isCompleted)
        assertFalse(todos.find { it.id == "d2" }!!.isCompleted)
    }

    @Test
    fun multiDishMealSlot() = runTest {
        val recipeDao = db.recipeDao()
        val mealPlanDao = db.mealPlanDao()

        recipeDao.insertRecipe(RecipeEntity(id = "main", name = "Grilled Chicken"))
        recipeDao.insertRecipe(RecipeEntity(id = "side1", name = "Rice"))
        recipeDao.insertRecipe(RecipeEntity(id = "side2", name = "Salad"))

        // Add main dish + two sides to same dinner slot
        mealPlanDao.addRecipeToSlot("2024-01-15", "dinner", "main")
        mealPlanDao.addRecipeToSlot("2024-01-15", "dinner", "side1")
        mealPlanDao.addRecipeToSlot("2024-01-15", "dinner", "side2")

        val slots = mealPlanDao.getMealSlotsForDateRange("2024-01-15", "2024-01-15").first()
        assertEquals(1, slots.size) // one slot
        assertEquals(3, slots[0].recipes.size) // three recipes

        // Remove side dish
        mealPlanDao.removeRecipeFromSlot("2024-01-15", "dinner", "side2")
        val updated = mealPlanDao.getMealSlotsForDateRange("2024-01-15", "2024-01-15").first()
        assertEquals(2, updated[0].recipes.size)
    }

    @Test
    fun recipeCategories_workflow() = runTest {
        val recipeDao = db.recipeDao()

        val cat = RecipeCategoryEntity(id = "c1", name = "Weeknight Dinners")
        recipeDao.insertCategory(cat)

        val categories = recipeDao.getAllCategories().first()
        assertEquals(1, categories.size)
        assertEquals("Weeknight Dinners", categories[0].name)
    }

    @Test
    fun shoppingListPreferencesWorkflow() = runTest {
        val recipeDao = db.recipeDao()
        val weekStart = "2024-01-15"

        // Save some preferences
        recipeDao.insertIngredientPreference(UserIngredientPreference(
            weekStartDate = weekStart,
            ingredientName = "onion",
            doNotBuy = true
        ))
        recipeDao.insertIngredientPreference(UserIngredientPreference(
            weekStartDate = weekStart,
            ingredientName = "garlic",
            needsChecking = true
        ))

        // Verify retrieval
        val prefs = recipeDao.getIngredientPreferences(weekStart).first()
        assertEquals(2, prefs.size)
        assertTrue(prefs.find { it.ingredientName == "onion" }!!.doNotBuy)
        assertTrue(prefs.find { it.ingredientName == "garlic" }!!.needsChecking)

        // Clear one
        recipeDao.deleteIngredientPreference(weekStart, "onion")
        val updated = recipeDao.getIngredientPreferences(weekStart).first()
        assertEquals(1, updated.size)
        assertEquals("garlic", updated[0].ingredientName)
    }

    @Test
    fun favoritesLogic_population() = runTest {
        val recipeDao = db.recipeDao()
        val mealPlanDao = db.mealPlanDao()
        
        // 1. Create standard Favorites tag
        recipeDao.insertTag(TagEntity(id = "fav-tag", name = "Favorites"))
        
        // 2. Create recipes: one high rated, one low
        recipeDao.insertRecipe(RecipeEntity(id = "r-high", name = "Good", rating = 5f))
        recipeDao.insertRecipe(RecipeEntity(id = "r-low", name = "Bad", rating = 2f))
        
        // 3. Add usage for the high rated one
        mealPlanDao.addRecipeToSlot("2024-01-15", "dinner", "r-high")
        
        // 4. Trigger repository logic (we simulate the repository call or just the Dao parts)
        val topRated = recipeDao.getRecipesByMinRating(4f)
        assertEquals(1, topRated.size)
        assertEquals("r-high", topRated[0].id)
        
        val usage = recipeDao.getRecipeUsageCountsSince(0) // all time for test
        assertTrue(usage.any { it.recipeId == "r-high" && it.count >= 1 })
    }
}
