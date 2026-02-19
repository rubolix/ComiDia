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

    private val _viewMode = MutableStateFlow(RecipeViewMode.LATEST)
    val viewMode: StateFlow<RecipeViewMode> = _viewMode

    private val recipesByLatest: StateFlow<List<RecipeWithTagsAndCategories>> =
        repository.getRecipesByLatest()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val recipesByName: StateFlow<List<RecipeWithTagsAndCategories>> =
        repository.getAllRecipesWithTagsAndCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<RecipeCategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter

    val filteredRecipes: StateFlow<List<RecipeWithTagsAndCategories>> = combine(
        recipesByLatest, recipesByName, _searchQuery, _selectedTagFilter, _viewMode
    ) { latest, byName, query, tagId, mode ->
        val source = if (mode == RecipeViewMode.LATEST) latest else byName
        source.filter { rwt ->
            val matchesQuery = query.isBlank() || rwt.recipe.name.contains(query, ignoreCase = true)
            val matchesTag = tagId == null || rwt.tags.any { it.id == tagId }
            matchesQuery && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun onTagFilterChange(tagId: String?) { _selectedTagFilter.value = tagId }

    fun setViewMode(mode: RecipeViewMode) { _viewMode.value = mode }

    fun deleteRecipe(id: String) {
        viewModelScope.launch { repository.deleteRecipe(id) }
    }

    fun archiveRecipe(id: String) {
        viewModelScope.launch { repository.archiveRecipe(id) }
    }

    fun copyRecipe(id: String) {
        viewModelScope.launch { repository.copyRecipe(id) }
    }
}
