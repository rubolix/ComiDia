package com.rubolix.comidia.data.local.dao

import androidx.room.*
import com.rubolix.comidia.data.local.entity.StapleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StapleDao {
    @Query("SELECT * FROM staples WHERE isRemoved = 0 ORDER BY category ASC, name ASC")
    fun getAllStaples(): Flow<List<StapleEntity>>

    @Query("SELECT * FROM staples ORDER BY category ASC, name ASC")
    fun getAllStaplesIncludingRemoved(): Flow<List<StapleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaple(staple: StapleEntity)

    @Update
    suspend fun updateStaple(staple: StapleEntity)

    @Delete
    suspend fun deleteStaple(staple: StapleEntity)

    @Query("UPDATE staples SET isRemoved = 1 WHERE id = :id")
    suspend fun markAsRemoved(id: String)

    @Query("UPDATE staples SET isRemoved = 0 WHERE id = :id")
    suspend fun restoreStaple(id: String)
}
