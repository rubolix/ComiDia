package com.rubolix.comidia.ui.screens.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.local.entity.*
import com.rubolix.comidia.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RecipeSortMode(val label: String) {
    BY_CATEGORIES("Categories"),
    ALL("All"),
    TOP_HITS("Top Hits"),
    ALPHABETICAL("A-Z"),
    BY_RATING("Rating"),
    BY_FREQUENCY("Frequency")
}

@OptIn(FlowPreview::class)
@HiltViewModel
class RecipeListViewModel @Inject constructor(
    private val repository: RecipeRepository,
    private val mealPlanRepository: com.rubolix.comidia.data.repository.MealPlanRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortMode = MutableStateFlow(RecipeSortMode.BY_CATEGORIES)
    val sortMode: StateFlow<RecipeSortMode> = _sortMode

    private val _allLimit = MutableStateFlow(20)
    val allLimit: StateFlow<Int> = _allLimit

    private val _topHitsLimit = MutableStateFlow(10)
    val topHitsLimit: StateFlow<Int> = _topHitsLimit

    private val _filterMinStars = MutableStateFlow(0)
    val filterMinStars: StateFlow<Int> = _filterMinStars

    private val _filterKidApproved = MutableStateFlow(false)
    val filterKidApproved: StateFlow<Boolean> = _filterKidApproved

    private val _filterMadeBefore = MutableStateFlow(false)
    val filterMadeBefore: StateFlow<Boolean> = _filterMadeBefore

    private val _filterFast = MutableStateFlow(false) 
    val filterFast: StateFlow<Boolean> = _filterFast

    private val _filterShortPrep = MutableStateFlow(false) 
    val filterShortPrep: StateFlow<Boolean> = _filterShortPrep

    private val _selectedTagIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedTagIds: StateFlow<Set<String>> = _selectedTagIds

    private val _pickerDate = MutableStateFlow<String?>(savedStateHandle["pickerDate"])
    val pickerDate: StateFlow<String?> = _pickerDate

    private val _pickerMealType = MutableStateFlow<String?>(savedStateHandle["pickerMealType"])
    val pickerMealType: StateFlow<String?> = _pickerMealType

    private val _alreadySelectedIds = MutableStateFlow<Set<String>>(emptySet())
    val alreadySelectedIds: StateFlow<Set<String>> = _alreadySelectedIds

    val isPickerMode = combine(_pickerDate, _pickerMealType) { d, t -> 
        d != null && t != null 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val selectedCategoryId: StateFlow<String?> = savedStateHandle.getStateFlow("selectedCategoryId", null)

    // Use null for "root" view
    val currentParentCategoryId: StateFlow<String?> = savedStateHandle.getStateFlow("currentParentCategoryId", null)

    val allRecipes: StateFlow<List<RecipeWithTagsAndCategories>> =
        repository.getAllRecipesWithTagsAndCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sourceLeftovers: StateFlow<List<com.rubolix.comidia.data.local.dao.MealPlanDao.RecipeWithUsage>> =
        combine(_pickerDate, _pickerMealType) { d, t -> d to t }.flatMapLatest { (dateStr, _) ->
            if (dateStr != null) {
                val date = java.time.LocalDate.parse(dateStr)
                val start = date.minusWeeks(2).toString()
                val end = date.plusDays(1).toString()
                mealPlanRepository.getSourceLeftoversForRange(start, end)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = repository.getAllTags()
        .map { list -> list.distinctBy { it.name.lowercase().trim() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<RecipeCategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class CategoryWithCount(
        val category: RecipeCategoryEntity,
        val count: Int
    )

    val displayCategories: StateFlow<List<CategoryWithCount>> = combine(
        allCategories, currentParentCategoryId, allRecipes
    ) { cats, parentId, recipes ->
        val list = cats.filter { it.parentId == parentId }.map { cat ->
            val familyIds = getCategoryFamilyIds(cat.id, cats)
            val count = recipes.count { r -> r.categories.any { it.id in familyIds } }
            CategoryWithCount(cat, count)
        }.toMutableList()
        
        // Add virtual "Uncategorized" category at root if there are uncategorized recipes
        if (parentId == null) {
            val uncategorizedRecipes = recipes.filter { it.categories.isEmpty() }
            if (uncategorizedRecipes.isNotEmpty()) {
                list.add(CategoryWithCount(
                    RecipeCategoryEntity(id = "virtual_uncategorized", name = "Uncategorized", sortOrder = Int.MAX_VALUE),
                    uncategorizedRecipes.size
                ))
            }
        }
        list.sortedBy { it.category.sortOrder }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.updateFavoritesTag()
        }
    }

    private val debouncedSearchQuery = _searchQuery
        .debounce(300)
        .distinctUntilChanged()

    private val filterState = combine(
        debouncedSearchQuery, _sortMode, _selectedTagIds, selectedCategoryId, 
        _filterMinStars, _filterKidApproved, _filterMadeBefore, _filterFast, _filterShortPrep,
        _allLimit, _topHitsLimit, _alreadySelectedIds
    ) { args -> args }

    val filteredRecipes: StateFlow<List<RecipeWithTagsAndCategories>> = combine(
        allRecipes, filterState
    ) { recipes, filters ->
        val query = filters[0] as String
        val sort = filters[1] as RecipeSortMode
        val tagIds = filters[2] as Set<String>
        val catId = filters[3] as String?
        val minStars = filters[4] as Int
        val kid = filters[5] as Boolean
        val made = filters[6] as Boolean
        val fast = filters[7] as Boolean
        val shortPrep = filters[8] as Boolean
        val allLimit = filters[9] as Int
        val topHitsLimit = filters[10] as Int
        val alreadySelected = filters[11] as Set<String>

        recipes.filter { rwt ->
            if (alreadySelected.contains(rwt.recipe.id)) return@filter false
            
            val matchesQuery = query.isBlank() || rwt.recipe.name.contains(query, ignoreCase = true)
            val matchesTags = tagIds.isEmpty() || tagIds.all { tid -> rwt.tags.any { it.id == tid } }
            
            val matchesCat = when (catId) {
                null -> true
                "virtual_uncategorized" -> rwt.categories.isEmpty()
                else -> {
                    val targetCatIds = getCategoryFamilyIds(catId, allCategories.value)
                    rwt.categories.any { it.id in targetCatIds }
                }
            }
            
            val matchesStars = rwt.recipe.rating >= minStars
            val matchesKid = !kid || rwt.recipe.isKidApproved
            val matchesMade = !made || rwt.recipe.rating > 0f 
            val matchesFast = !fast || (rwt.recipe.totalTimeMinutes in 1..30)
            val matchesShortPrep = !shortPrep || (rwt.recipe.prepTimeMinutes in 1..20)

            matchesQuery && matchesTags && matchesCat && matchesStars && matchesKid && matchesMade && matchesFast && matchesShortPrep
        }.distinctBy { it.recipe.id }
        .let { filtered ->
            when (sort) {
                RecipeSortMode.ALL -> filtered.sortedByDescending { it.recipe.updatedAt }.take(allLimit)
                RecipeSortMode.TOP_HITS -> filtered.sortedByDescending { it.recipe.rating }.take(topHitsLimit)
                RecipeSortMode.ALPHABETICAL -> filtered.sortedBy { it.recipe.name.lowercase() }
                RecipeSortMode.BY_RATING -> filtered.sortedByDescending { it.recipe.rating }
                RecipeSortMode.BY_FREQUENCY -> filtered 
                RecipeSortMode.BY_CATEGORIES -> filtered
            }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun setSortMode(mode: RecipeSortMode) { 
        _sortMode.value = mode
        _allLimit.value = 20
        _topHitsLimit.value = 10
        if (mode != RecipeSortMode.BY_CATEGORIES) {
            savedStateHandle["selectedCategoryId"] = null
            savedStateHandle["currentParentCategoryId"] = null
        }
    }

    fun loadMoreAll() { _allLimit.update { it + 20 } }
    fun loadMoreTopHits() { _topHitsLimit.update { it + 10 } }
    
    fun setMinStars(stars: Int) { _filterMinStars.value = stars }
    fun toggleKidApproved() { _filterKidApproved.value = !_filterKidApproved.value }
    fun toggleMadeBefore() { _filterMadeBefore.value = !_filterMadeBefore.value }
    fun toggleFast() { _filterFast.value = !_filterFast.value }
    fun toggleShortPrep() { _filterShortPrep.value = !_filterShortPrep.value }
    fun toggleTag(tagId: String) {
        _selectedTagIds.update { current ->
            if (tagId in current) current - tagId else current + tagId
        }
    }
    fun clearTags() { _selectedTagIds.value = emptySet() }

    fun selectCategory(catId: String?) {
        if (catId == null) {
            savedStateHandle["selectedCategoryId"] = null
            return
        }
        
        val cat = allCategories.value.find { it.id == catId } ?: if (catId == "virtual_uncategorized") RecipeCategoryEntity(id="virtual_uncategorized", name="Uncategorized") else return
        val hasChildren = allCategories.value.any { it.parentId == cat.id }
        
        if (hasChildren) {
            savedStateHandle["currentParentCategoryId"] = cat.id
            savedStateHandle["selectedCategoryId"] = null // Stay in grid view but one level deeper
        } else {
            savedStateHandle["selectedCategoryId"] = cat.id // Show recipes for this leaf
        }
    }

    fun navigateUp() {
        val currentParent = currentParentCategoryId.value ?: return
        if (selectedCategoryId.value != null) {
            savedStateHandle["selectedCategoryId"] = null // Back to grid
        } else {
            val parent = allCategories.value.find { it.id == currentParent }
            savedStateHandle["currentParentCategoryId"] = parent?.parentId
        }
    }

    fun clearAllFilters() {
        _filterMinStars.value = 0
        _filterKidApproved.value = false
        _filterMadeBefore.value = false
        _filterFast.value = false
        _filterShortPrep.value = false
        _selectedTagIds.value = emptySet()
        savedStateHandle["selectedCategoryId"] = null
        savedStateHandle["currentParentCategoryId"] = null
    }

    fun deleteRecipe(id: String) { viewModelScope.launch { repository.deleteRecipe(id) } }
    fun archiveRecipe(id: String) { viewModelScope.launch { repository.archiveRecipe(id) } }
    fun copyRecipe(id: String) { viewModelScope.launch { repository.copyRecipe(id) } }

    fun setPickerMode(date: String?, mealType: String?, alreadySelected: Set<String> = emptySet()) {
        _pickerDate.value = date
        _pickerMealType.value = mealType
        _alreadySelectedIds.value = alreadySelected
    }

    fun onSelectFlexibleMeal(title: String, type: String, onResult: (String, String, String, String?) -> Unit) {
        val date = _pickerDate.value ?: return
        val mealType = _pickerMealType.value ?: return
        onResult(date, mealType, title, type)
    }

    fun onSelectLeftover(recipeId: String, onResult: (String, String, String) -> Unit) {
        val date = _pickerDate.value ?: return
        val mealType = _pickerMealType.value ?: return
        onResult(date, mealType, recipeId)
    }

    private fun getCategoryFamilyIds(parentId: String, allCats: List<RecipeCategoryEntity>): Set<String> {
        val result = mutableSetOf(parentId)
        val children = allCats.filter { it.parentId == parentId }
        children.forEach { child ->
            result.addAll(getCategoryFamilyIds(child.id, allCats))
        }
        return result
    }
}
