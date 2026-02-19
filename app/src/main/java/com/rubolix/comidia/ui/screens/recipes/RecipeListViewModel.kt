package com.rubolix.comidia.ui.screens.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.local.entity.*
import com.rubolix.comidia.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipeListViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val recipes: StateFlow<List<RecipeWithTags>> = repository.getAllRecipesWithTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter

    val filteredRecipes: StateFlow<List<RecipeWithTags>> = combine(
        recipes, _searchQuery, _selectedTagFilter
    ) { allRecipes, query, tagId ->
        allRecipes.filter { recipeWithTags ->
            val matchesQuery = query.isBlank() || recipeWithTags.recipe.name.contains(query, ignoreCase = true)
            val matchesTag = tagId == null || recipeWithTags.tags.any { it.id == tagId }
            matchesQuery && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTagFilterChange(tagId: String?) {
        _selectedTagFilter.value = tagId
    }

    fun deleteRecipe(id: String) {
        viewModelScope.launch {
            repository.deleteRecipe(id)
        }
    }
}
