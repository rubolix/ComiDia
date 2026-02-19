package com.rubolix.comidia.ui.screens.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.local.dao.RecipeDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class IngredientSummary(
    val name: String,
    val category: String,
    val recipeCount: Int
)

@HiltViewModel
class IngredientsViewModel @Inject constructor(
    private val recipeDao: RecipeDao
) : ViewModel() {

    val ingredientsByCategory: StateFlow<Map<String, List<IngredientSummary>>> =
        recipeDao.getAllRecipesWithTags().map { recipes ->
            // We need ingredients, so let's query them differently
            emptyMap<String, List<IngredientSummary>>()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // TODO: Add a proper query to get all unique ingredients grouped by category with recipe count
}
