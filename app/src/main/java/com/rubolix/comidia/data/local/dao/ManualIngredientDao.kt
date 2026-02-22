package com.rubolix.comidia.data.local.dao

import androidx.room.*
import com.rubolix.comidia.data.local.entity.ManualIngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ManualIngredientDao {
    @Query("SELECT * FROM manual_ingredients WHERE weekStartDate = :weekStart")
    fun getManualIngredientsForWeek(weekStart: String): Flow<List<ManualIngredientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManualIngredient(ingredient: ManualIngredientEntity)

    @Delete
    suspend fun deleteManualIngredient(ingredient: ManualIngredientEntity)
}
