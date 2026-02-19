package com.rubolix.comidia.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rubolix.comidia.data.local.dao.ManualIngredientDao
import com.rubolix.comidia.data.local.entity.ManualIngredientEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualIngredientDaoTest {

    private lateinit var db: ComiDiaDatabase
    private lateinit var dao: ManualIngredientDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ComiDiaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.manualIngredientDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetManualIngredients() = runTest {
        val ing = ManualIngredientEntity(
            weekStartDate = "2024-01-15",
            name = "Milk",
            quantity = "1",
            unit = "gallon"
        )
        dao.insertManualIngredient(ing)

        val list = dao.getManualIngredientsForWeek("2024-01-15").first()
        assertEquals(1, list.size)
        assertEquals("Milk", list[0].name)
    }

    @Test
    fun deleteManualIngredient() = runTest {
        val ing = ManualIngredientEntity(id = "m1", weekStartDate = "2024-01-15", name = "Eggs")
        dao.insertManualIngredient(ing)
        dao.deleteManualIngredient(ing)

        val list = dao.getManualIngredientsForWeek("2024-01-15").first()
        assertTrue(list.isEmpty())
    }
}
