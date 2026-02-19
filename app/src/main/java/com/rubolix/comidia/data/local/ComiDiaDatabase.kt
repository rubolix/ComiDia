package com.rubolix.comidia.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rubolix.comidia.data.local.dao.MealPlanDao
import com.rubolix.comidia.data.local.dao.RecipeDao
import com.rubolix.comidia.data.local.entity.*

@Database(
    entities = [
        RecipeEntity::class,
        IngredientEntity::class,
        TagEntity::class,
        RecipeTagCrossRef::class,
        MealSlotEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ComiDiaDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun mealPlanDao(): MealPlanDao
}
