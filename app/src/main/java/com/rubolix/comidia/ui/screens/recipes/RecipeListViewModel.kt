package com.rubolix.comidia.ui.screens.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.local.entity.*
import com.rubolix.comidia.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RecipeSortMode(val label: String) {
    LATEST("Latest"),
    TOP_HITS("Top Hits"),
    ALPHABETICAL("A-Z"),
    BY_RATING("Rating"),
    BY_FREQUENCY("Frequency")
}

@HiltViewModel
class RecipeListViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortMode = MutableStateFlow(RecipeSortMode.LATEST)
    val sortMode: StateFlow<RecipeSortMode> = _sortMode

    private val _selectedTagId = MutableStateFlow<String?>(null)
    val selectedTagId: StateFlow<String?> = _selectedTagId

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId

    // View State: show categories grid or flat list
    private val _showCategoriesGrid = MutableStateFlow(true)
    val showCategoriesGrid: StateFlow<Boolean> = _showCategoriesGrid

    val allRecipes: StateFlow<List<RecipeWithTagsAndCategories>> =
        repository.getAllRecipesWithTagsAndCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<RecipeCategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.updateFavoritesTag()
            // If no categories exist, default to flat list
            categories.collect { if (it.isEmpty()) _showCategoriesGrid.value = false }
        }
    }

    val filteredRecipes: StateFlow<List<RecipeWithTagsAndCategories>> = combine(
        allRecipes, _searchQuery, _sortMode, _selectedTagId, _selectedCategoryId
    ) { recipes, query, sort, tagId, catId ->
        var result = recipes.filter { rwt ->
            val matchesQuery = query.isBlank() || rwt.recipe.name.contains(query, ignoreCase = true)
            val matchesTag = tagId == null || rwt.tags.any { it.id == tagId }
            val matchesCat = catId == null || rwt.categories.any { it.id == catId }
            matchesQuery && matchesTag && matchesCat
        }

        result = when (sort) {
            RecipeSortMode.LATEST -> result.sortedByDescending { it.recipe.updatedAt }
            RecipeSortMode.ALPHABETICAL -> result.sortedBy { it.recipe.name.lowercase() }
            RecipeSortMode.BY_RATING -> result.sortedByDescending { it.recipe.rating }
            RecipeSortMode.TOP_HITS -> result.sortedByDescending { it.recipe.rating } // Add frequency logic later if needed
            RecipeSortMode.BY_FREQUENCY -> result // Placeholder
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun setSortMode(mode: RecipeSortMode) { 
        _sortMode.value = mode 
        _showCategoriesGrid.value = false
    }
    fun selectTag(tagId: String?) { _selectedTagId.value = tagId }
    fun selectCategory(catId: String?) { 
        _selectedCategoryId.value = catId 
        if (catId != null) _showCategoriesGrid.value = false
    }
    fun setShowCategoriesGrid(show: Boolean) { 
        _showCategoriesGrid.value = show 
        if (show) {
            _selectedCategoryId.value = null
            _selectedTagId.value = null
        }
    }

    fun deleteRecipe(id: String) { viewModelScope.launch { repository.deleteRecipe(id) } }
    fun archiveRecipe(id: String) { viewModelScope.launch { repository.archiveRecipe(id) } }
    fun copyRecipe(id: String) { viewModelScope.launch { repository.copyRecipe(id) } }
}
