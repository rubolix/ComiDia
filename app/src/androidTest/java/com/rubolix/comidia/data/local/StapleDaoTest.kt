package com.rubolix.comidia.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rubolix.comidia.data.local.dao.StapleDao
import com.rubolix.comidia.data.local.entity.StapleEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StapleDaoTest {

    private lateinit var db: ComiDiaDatabase
    private lateinit var stapleDao: StapleDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ComiDiaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        stapleDao = db.stapleDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetStaples() = runTest {
        val staple = StapleEntity(id = "s1", name = "Milk", category = "Dairy")
        stapleDao.insertStaple(staple)

        val list = stapleDao.getAllStaples().first()
        assertEquals(1, list.size)
        assertEquals("Milk", list[0].name)
    }

    @Test
    fun markAsRemoved_filtersFromActiveList() = runTest {
        stapleDao.insertStaple(StapleEntity(id = "s1", name = "Milk"))
        stapleDao.markAsRemoved("s1")

        val active = stapleDao.getAllStaples().first()
        assertTrue(active.isEmpty())

        val all = stapleDao.getAllStaplesIncludingRemoved().first()
        assertEquals(1, all.size)
        assertTrue(all[0].isRemoved)
    }

    @Test
    fun restoreStaple_returnsToActiveList() = runTest {
        stapleDao.insertStaple(StapleEntity(id = "s1", name = "Milk", isRemoved = true))
        stapleDao.restoreStaple("s1")

        val active = stapleDao.getAllStaples().first()
        assertEquals(1, active.size)
        assertFalse(active[0].isRemoved)
    }
}
