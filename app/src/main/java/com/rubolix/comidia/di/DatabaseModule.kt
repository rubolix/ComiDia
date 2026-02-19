package com.rubolix.comidia.di

import android.content.Context
import androidx.room.Room
import com.rubolix.comidia.data.local.ComiDiaDatabase
import com.rubolix.comidia.data.local.SeedDatabaseCallback
import com.rubolix.comidia.data.local.dao.MealPlanDao
import com.rubolix.comidia.data.local.dao.RecipeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ComiDiaDatabase {
        return Room.databaseBuilder(
            context,
            ComiDiaDatabase::class.java,
            "comidia.db"
        )
            .addCallback(SeedDatabaseCallback(context))
            .build()
    }

    @Provides
    fun provideRecipeDao(db: ComiDiaDatabase): RecipeDao = db.recipeDao()

    @Provides
    fun provideMealPlanDao(db: ComiDiaDatabase): MealPlanDao = db.mealPlanDao()
}
