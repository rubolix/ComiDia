package com.rubolix.comidia

import android.app.Application
import com.rubolix.comidia.data.local.ComiDiaDatabase
import com.rubolix.comidia.data.local.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ComiDiaApp : Application() {
    @Inject
    lateinit var database: ComiDiaDatabase

    override fun onCreate() {
        super.onCreate()
        
        // Auto-seed default categories if empty
        CoroutineScope(Dispatchers.IO).launch {
            val categories = database.recipeDao().getAllCategories().first()
            if (categories.isEmpty()) {
                database.runInTransaction {
                    DatabaseSeeder.seedDefaultCategories(database.openHelper.writableDatabase)
                }
            }
        }
    }
}
