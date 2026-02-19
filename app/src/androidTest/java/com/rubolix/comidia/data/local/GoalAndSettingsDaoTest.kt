package com.rubolix.comidia.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rubolix.comidia.data.local.dao.GoalDao
import com.rubolix.comidia.data.local.dao.SettingsDao
import com.rubolix.comidia.data.local.entity.AppSettingsEntity
import com.rubolix.comidia.data.local.entity.MealPlanGoalEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoalAndSettingsDaoTest {

    private lateinit var db: ComiDiaDatabase
    private lateinit var goalDao: GoalDao
    private lateinit var settingsDao: SettingsDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ComiDiaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        goalDao = db.goalDao()
        settingsDao = db.settingsDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // === Goal DAO Tests ===

    @Test
    fun insertAndRetrieveGoal() = runTest {
        val goal = MealPlanGoalEntity(
            id = "g1",
            description = "Fish",
            tagId = "fish-tag",
            goalType = "gte",
            targetCount = 1,
            period = "week"
        )
        goalDao.insertGoal(goal)

        val goals = goalDao.getAllGoals().first()
        assertEquals(1, goals.size)
        assertEquals("Fish", goals[0].description)
        assertEquals("gte", goals[0].goalType)
        assertEquals("week", goals[0].period)
    }

    @Test
    fun getActiveGoals_filtersInactive() = runTest {
        goalDao.insertGoal(MealPlanGoalEntity(id = "g1", description = "Active", goalType = "eq", targetCount = 1, isActive = true))
        goalDao.insertGoal(MealPlanGoalEntity(id = "g2", description = "Inactive", goalType = "eq", targetCount = 1, isActive = false))

        val active = goalDao.getActiveGoals().first()
        assertEquals(1, active.size)
        assertEquals("Active", active[0].description)
    }

    @Test
    fun toggleGoalActive() = runTest {
        goalDao.insertGoal(MealPlanGoalEntity(id = "g1", description = "Test", goalType = "eq", targetCount = 1, isActive = true))

        goalDao.setGoalActive("g1", false)
        var goals = goalDao.getAllGoals().first()
        assertFalse(goals[0].isActive)

        goalDao.setGoalActive("g1", true)
        goals = goalDao.getAllGoals().first()
        assertTrue(goals[0].isActive)
    }

    @Test
    fun deleteGoal() = runTest {
        val goal = MealPlanGoalEntity(id = "g1", description = "Test", goalType = "eq", targetCount = 1)
        goalDao.insertGoal(goal)
        goalDao.deleteGoal(goal)

        val goals = goalDao.getAllGoals().first()
        assertEquals(0, goals.size)
    }

    @Test
    fun goalWithNewFields_comparison_and_period() = runTest {
        goalDao.insertGoal(MealPlanGoalEntity(
            id = "g1", description = "Fish", goalType = "lte",
            targetCount = 2, period = "month"
        ))

        val goals = goalDao.getAllGoals().first()
        assertEquals("lte", goals[0].goalType)
        assertEquals("month", goals[0].period)
    }

    // === Settings DAO Tests ===

    @Test
    fun setAndGetSetting() = runTest {
        settingsDao.setSetting(AppSettingsEntity("test_key", "test_value"))

        val setting = settingsDao.getSetting("test_key").first()
        assertNotNull(setting)
        assertEquals("test_value", setting!!.value)
    }

    @Test
    fun getSetting_nonExistent_returnsNull() = runTest {
        val setting = settingsDao.getSetting("nonexistent").first()
        assertNull(setting)
    }

    @Test
    fun setSetting_overwrites() = runTest {
        settingsDao.setSetting(AppSettingsEntity("key", "old"))
        settingsDao.setSetting(AppSettingsEntity("key", "new"))

        val setting = settingsDao.getSetting("key").first()
        assertEquals("new", setting!!.value)
    }

    @Test
    fun getAllSettings() = runTest {
        settingsDao.setSetting(AppSettingsEntity("key1", "val1"))
        settingsDao.setSetting(AppSettingsEntity("key2", "val2"))

        val all = settingsDao.getAllSettings().first()
        assertEquals(2, all.size)
    }

    @Test
    fun firstDayOfWeek_setting() = runTest {
        settingsDao.setSetting(AppSettingsEntity(AppSettingsEntity.FIRST_DAY_OF_WEEK, "sunday"))

        val setting = settingsDao.getSetting(AppSettingsEntity.FIRST_DAY_OF_WEEK).first()
        assertEquals("sunday", setting!!.value)
    }

    @Test
    fun defaultMealTypes_setting() = runTest {
        settingsDao.setSetting(AppSettingsEntity(AppSettingsEntity.DEFAULT_MEAL_TYPES, "breakfast,dinner"))

        val setting = settingsDao.getSetting(AppSettingsEntity.DEFAULT_MEAL_TYPES).first()
        val types = setting!!.value.split(",").toSet()
        assertEquals(setOf("breakfast", "dinner"), types)
    }
}
