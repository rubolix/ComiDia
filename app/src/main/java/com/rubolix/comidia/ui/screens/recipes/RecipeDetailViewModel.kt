package com.rubolix.comidia.ui.screens.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.local.entity.RecipeFull
import com.rubolix.comidia.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val repository: RecipeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _recipeId = MutableStateFlow(savedStateHandle.get<String>("recipeId") ?: "")
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val recipeFull: StateFlow<RecipeFull?> = _recipeId.flatMapLatest { id ->
        repository.getRecipeFull(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setRecipeId(id: String) {
        _recipeId.value = id
    }

    fun deleteRecipe() {
        viewModelScope.launch { repository.deleteRecipe(_recipeId.value) }
    }

    fun archiveRecipe() {
        viewModelScope.launch { repository.archiveRecipe(_recipeId.value) }
    }

    fun copyRecipe() {
        viewModelScope.launch { repository.copyRecipe(_recipeId.value) }
    }

    fun updateRating(rating: Float) {
        viewModelScope.launch {
            val full = recipeFull.value ?: return@launch
            repository.saveRecipe(full.recipe.copy(rating = rating), full.ingredients, full.tags.map { it.id }, full.categories.map { it.id })
        }
    }

    fun updateKidApproved(approved: Boolean) {
        viewModelScope.launch {
            val full = recipeFull.value ?: return@launch
            repository.saveRecipe(full.recipe.copy(isKidApproved = approved), full.ingredients, full.tags.map { it.id }, full.categories.map { it.id })
        }
    }
}
