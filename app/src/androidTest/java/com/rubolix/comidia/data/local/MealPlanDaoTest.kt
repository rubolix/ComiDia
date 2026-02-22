package com.rubolix.comidia.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rubolix.comidia.data.local.dao.MealPlanDao
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
class MealPlanDaoTest {

    private lateinit var db: ComiDiaDatabase
    private lateinit var mealPlanDao: MealPlanDao
    private lateinit var recipeDao: RecipeDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ComiDiaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mealPlanDao = db.mealPlanDao()
        recipeDao = db.recipeDao()

        // Seed some recipes
        kotlinx.coroutines.runBlocking {
            recipeDao.insertRecipe(RecipeEntity(id = "r1", name = "Chicken Tacos"))
            recipeDao.insertRecipe(RecipeEntity(id = "r2", name = "Beef Stew"))
            recipeDao.insertRecipe(RecipeEntity(id = "r3", name = "Salad"))
        }
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun addRecipeToSlot_createsSlotAndLink() = runTest {
        mealPlanDao.addRecipeToSlot("2024-01-15", "dinner", "r1")

        val slots = mealPlanDao.getMealSlotsForDateRange("2024-01-15", "2024-01-15").first()
        assertEquals(1, slots.size)
        assertEquals("dinner", slots[0].mealSlot.mealType)
        assertEquals(1, slots[0].recipes.size)
        assertEquals("Chicken Tacos", slots[0].recipes[0].name)
    }

    @Test
    fun addMultipleRecipesToSameSlot() = runTest {
        mealPlanDao.addRecipeToSlot("2024-01-15", "dinner", "r1")
        mealPlanDao.addRecipeToSlot("2024-01-15", "dinner", "r2")

        val slots = mealPlanDao.getMealSlotsForDateRange("2024-01-15", "2024-01-15").first()
        assertEquals(1, slots.size)
        assertEquals(2, slots[0].recipes.size)
    }

    @Test
    fun removeRecipeFromSlot_removesLink() = runTest {
        mealPlanDao.addRecipeToSlot("2024-01-15", "dinner", "r1")
        mealPlanDao.addRecipeToSlot("2024-01-15", "dinner", "r2")
        mealPlanDao.removeRecipeFromSlot("2024-01-15", "dinner", "r1")

        val slots = mealPlanDao.getMealSlotsForDateRange("2024-01-15", "2024-01-15").first()
        assertEquals(1, slots.size)
        assertEquals(1, slots[0].recipes.size)
        assertEquals("Beef Stew", slots[0].recipes[0].name)
    }

    @Test
    fun removeLastRecipe_deletesSlot() = runTest {
        mealPlanDao.addRecipeToSlot("2024-01-15", "dinner", "r1")
        mealPlanDao.removeRecipeFromSlot("2024-01-15", "dinner", "r1")

        val slots = mealPlanDao.getMealSlotsForDateRange("2024-01-15", "2024-01-15").first()
        assertEquals(0, slots.size)
    }

    @Test
    fun getMealSlotsForDateRange_filtersCorrectly() = runTest {
        mealPlanDao.addRecipeToSlot("2024-01-15", "dinner", "r1")
        mealPlanDao.addRecipeToSlot("2024-01-16", "lunch", "r2")
        mealPlanDao.addRecipeToSlot("2024-01-20", "dinner", "r3")

        val slots = mealPlanDao.getMealSlotsForDateRange("2024-01-15", "2024-01-16").first()
        assertEquals(2, slots.size)
    }

    @Test
    fun weeklyItems_crudOperations() = runTest {
        val item = WeeklyItemEntity(id = "w1", weekStartDate = "2024-01-15", text = "Buy fruit")
        mealPlanDao.insertWeeklyItem(item)

        var items = mealPlanDao.getWeeklyItems("2024-01-15").first()
        assertEquals(1, items.size)
        assertEquals("Buy fruit", items[0].text)
        assertFalse(items[0].isCompleted)

        mealPlanDao.setWeeklyItemCompleted("w1", true)
        items = mealPlanDao.getWeeklyItems("2024-01-15").first()
        assertTrue(items[0].isCompleted)

        mealPlanDao.deleteWeeklyItem(items[0])
        items = mealPlanDao.getWeeklyItems("2024-01-15").first()
        assertEquals(0, items.size)
    }

    @Test
    fun dailyTodos_crudOperations() = runTest {
        val todo = DailyTodoEntity(id = "d1", date = "2024-01-15", text = "Defrost chicken")
        mealPlanDao.insertDailyTodo(todo)

        var todos = mealPlanDao.getDailyTodosForRange("2024-01-15", "2024-01-15").first()
        assertEquals(1, todos.size)
        assertFalse(todos[0].isCompleted)

        mealPlanDao.setDailyTodoCompleted("d1", true)
        todos = mealPlanDao.getDailyTodosForRange("2024-01-15", "2024-01-15").first()
        assertTrue(todos[0].isCompleted)

        mealPlanDao.deleteDailyTodo(todos[0])
        todos = mealPlanDao.getDailyTodosForRange("2024-01-15", "2024-01-15").first()
        assertEquals(0, todos.size)
    }

    @Test
    fun dailyTodos_filteredByDateRange() = runTest {
        mealPlanDao.insertDailyTodo(DailyTodoEntity(date = "2024-01-15", text = "Task 1"))
        mealPlanDao.insertDailyTodo(DailyTodoEntity(date = "2024-01-16", text = "Task 2"))
        mealPlanDao.insertDailyTodo(DailyTodoEntity(date = "2024-01-20", text = "Task 3"))

        val todos = mealPlanDao.getDailyTodosForRange("2024-01-15", "2024-01-16").first()
        assertEquals(2, todos.size)
    }

    @Test
    fun differentMealTypesOnSameDay() = runTest {
        mealPlanDao.addRecipeToSlot("2024-01-15", "breakfast", "r1")
        mealPlanDao.addRecipeToSlot("2024-01-15", "lunch", "r2")
        mealPlanDao.addRecipeToSlot("2024-01-15", "dinner", "r3")

        val slots = mealPlanDao.getMealSlotsForDateRange("2024-01-15", "2024-01-15").first()
        assertEquals(3, slots.size)
        val mealTypes = slots.map { it.mealSlot.mealType }.toSet()
        assertTrue(mealTypes.containsAll(setOf("breakfast", "lunch", "dinner")))
    }

    @Test
    fun addRecipeWithLeftoverFlag() = runTest {
        mealPlanDao.addRecipeToSlot("2024-01-15", "dinner", "r1", generatesLeftovers = true)
        
        val slots = mealPlanDao.getMealSlotsForDateRange("2024-01-15", "2024-01-15").first()
        val ref = slots[0].recipeRefs[0]
        assertTrue(ref.generatesLeftovers)
        assertFalse(ref.isLeftover)
    }

    @Test
    fun addCustomEntryToSlot_works() = runTest {
        mealPlanDao.addCustomEntryToSlot("2024-01-15", "dinner", "Pizza", "takeout")
        
        val slots = mealPlanDao.getMealSlotsForDateRange("2024-01-15", "2024-01-15").first()
        assertEquals(1, slots[0].customEntries.size)
        assertEquals("Pizza", slots[0].customEntries[0].title)
        assertEquals("takeout", slots[0].customEntries[0].type)
    }

    @Test
    fun getSourceLeftoversForRange_filtersCorrectly() = runTest {
        mealPlanDao.addRecipeToSlot("2024-01-15", "dinner", "r1", generatesLeftovers = true)
        mealPlanDao.addRecipeToSlot("2024-01-16", "dinner", "r2", generatesLeftovers = false)
        
        val leftovers = mealPlanDao.getSourceLeftoversForRange("2024-01-15", "2024-01-31").first()
        assertEquals(1, leftovers.size)
        assertEquals("r1", leftovers[0].recipe.id)
    }

    @Test
    fun weekMetadata_persistence() = runTest {
        val meta = WeekMetadata("2024-01-15", true)
        mealPlanDao.insertWeekMetadata(meta)

        val retrieved = mealPlanDao.getWeekMetadata("2024-01-15").first()
        assertNotNull(retrieved)
        assertTrue(retrieved!!.availabilityPassDone)
    }
}
