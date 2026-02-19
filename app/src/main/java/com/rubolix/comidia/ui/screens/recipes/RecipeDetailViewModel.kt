package com.rubolix.comidia.ui.screens.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.local.entity.RecipeFull
import com.rubolix.comidia.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val repository: RecipeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val recipeId: String = savedStateHandle.get<String>("recipeId") ?: ""

    val recipeFull: StateFlow<RecipeFull?> = repository.getRecipeFull(recipeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun deleteRecipe() {
        viewModelScope.launch { repository.deleteRecipe(recipeId) }
    }

    fun archiveRecipe() {
        viewModelScope.launch { repository.archiveRecipe(recipeId) }
    }

    fun copyRecipe() {
        viewModelScope.launch { repository.copyRecipe(recipeId) }
    }
}
