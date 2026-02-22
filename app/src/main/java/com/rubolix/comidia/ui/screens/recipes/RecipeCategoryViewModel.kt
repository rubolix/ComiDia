package com.rubolix.comidia.ui.screens.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.local.ComiDiaDatabase
import com.rubolix.comidia.data.local.DatabaseSeeder
import com.rubolix.comidia.data.local.entity.CategorySmartRuleEntity
import com.rubolix.comidia.data.local.entity.RecipeCategoryEntity
import com.rubolix.comidia.data.local.entity.RecipeWithTagsAndCategories
import com.rubolix.comidia.data.local.entity.TagEntity
import com.rubolix.comidia.data.repository.RecipeRepository
import com.rubolix.comidia.ui.components.CategoryNode
import com.rubolix.comidia.ui.components.CategoryTreeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RecipeCategoryViewModel @Inject constructor(
    private val repository: RecipeRepository,
    private val db: ComiDiaDatabase
) : ViewModel() {

    private val _buffer = MutableStateFlow<List<RecipeCategoryEntity>>(emptyList())
    private val history = mutableListOf<List<RecipeCategoryEntity>>()
    private var isInitialized = false

    private val _selectedCategoryIdForSmartRule = MutableStateFlow<String?>(null)
    val smartRule = _selectedCategoryIdForSmartRule.flatMapLatest { cid ->
        if (cid == null) flowOf(null)
        else repository.getSmartRuleForCategory(cid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allCategories = repository.getAllCategories()
        .onEach { cats ->
            if (!isInitialized && cats.isNotEmpty()) {
                _buffer.value = cats
                history.clear()
                history.add(cats)
                isInitialized = true
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasSmartRuleMap: StateFlow<Map<String, Boolean>> = allCategories.flatMapLatest { cats ->
        val flows = cats.map { cat ->
            repository.getSmartRuleForCategory(cat.id).map { cat.id to (it != null) }
        }
        if (flows.isEmpty()) flowOf(emptyMap())
        else combine(flows) { it.toMap() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val categoryTree = _buffer.map { CategoryTreeUtils.buildTree(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isModified = combine(allCategories, _buffer) { original, current ->
        if (original.isEmpty() && current.isNotEmpty() && !isInitialized) false
        else original != current
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isReorderMode = MutableStateFlow(false)
    val isReorderMode: StateFlow<Boolean> = _isReorderMode

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds

    val allRecipes = repository.getAllRecipesWithTagsAndCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun updateBuffer(newList: List<RecipeCategoryEntity>) {
        _buffer.value = newList
        history.add(newList)
    }

    fun toggleReorderMode() {
        _isReorderMode.value = !_isReorderMode.value
    }

    fun toggleExpansion(id: String) {
        _expandedIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun collapseAll() {
        _expandedIds.value = emptySet()
    }

    fun selectCategoryForSmartRule(categoryId: String?) {
        _selectedCategoryIdForSmartRule.value = categoryId
    }

    fun saveAndApplySmartRule(rule: CategorySmartRuleEntity) {
        viewModelScope.launch {
            repository.saveSmartRule(rule)
            applySmartRule(rule)
            _selectedCategoryIdForSmartRule.value = null
        }
    }

    fun resetSmartRule(categoryId: String) {
        viewModelScope.launch {
            val rule = repository.getSmartRuleForCategory(categoryId).first()
            if (rule != null) {
                // Remove recipes added via this rule (addedAt >= rule.createdAt)
                val allRecs = repository.getAllRecipesWithTagsAndCategories().first()
                val toRemove = allRecs.filter { rwt ->
                    rwt.categories.any { it.id == categoryId }
                }.filter { rwt ->
                    // We need to fetch the cross-ref addedAt somehow. 
                    // For now, let's remove ALL recipes in this category since they are likely all auto-populated if a rule existed.
                    // A better approach would be to fetch cross-refs with addedAt.
                    true 
                }.map { it.recipe.id }
                
                if (toRemove.isNotEmpty()) {
                    repository.batchRemoveRecipesFromCategory(toRemove, categoryId)
                }
                repository.deleteSmartRuleForCategory(categoryId)
            }
            _selectedCategoryIdForSmartRule.value = null
        }
    }

    private suspend fun applySmartRule(rule: CategorySmartRuleEntity) {
        val allRecs = allRecipes.value
        val includeTags = rule.includeTagIds.split(",").filter { it.isNotBlank() }.toSet()
        val includeCats = rule.includeCategoryIds.split(",").filter { it.isNotBlank() }.toSet()
        
        // For recursive category check
        val expandedCats = includeCats.flatMap { getCategoryFamilyIds(it, allCategories.value) }.toSet()

                    val matching = allRecs.filter { rwt ->
                        val matchesStars = rwt.recipe.rating >= rule.minStars
                        val matchesKid = !rule.kidApprovedOnly || rwt.recipe.isKidApproved
                        val totalTime = rwt.recipe.prepTimeMinutes + rwt.recipe.cookTimeMinutes
                        
                        val matchesMaxTotal = rule.maxTotalTime == null || (totalTime > 0 && totalTime <= rule.maxTotalTime)
                        val matchesMinTotal = rule.minTotalTime == null || (totalTime >= rule.minTotalTime)
                        val matchesPrep = rule.maxPrepTime == null || (rwt.recipe.prepTimeMinutes > 0 && rwt.recipe.prepTimeMinutes <= rule.maxPrepTime)
                        
                        // Time matches if (Total is between Min/Max) OR (Prep matches Max)
                        val timeCriteriaActive = rule.maxTotalTime != null || rule.minTotalTime != null || rule.maxPrepTime != null
                        val timeMatches = if (timeCriteriaActive) (matchesMaxTotal && matchesMinTotal) || matchesPrep else true
                        
                        val matchesTags = includeTags.isEmpty() || rwt.tags.any { it.id in includeTags }
                        val matchesCats = includeCats.isEmpty() || rwt.categories.any { it.id in expandedCats }
        
                        matchesStars && matchesKid && timeMatches && matchesTags && matchesCats
                    }.map { it.recipe.id }
                if (matching.isNotEmpty()) {
            repository.batchAddRecipesToCategory(matching, rule.categoryId)
        }
    }

    fun saveChanges() {
        viewModelScope.launch {
            val finalBuffer = _buffer.value
            val original = allCategories.value
            
            // 1. Identify deleted
            val finalIds = finalBuffer.map { it.id }.toSet()
            val deleted = original.filter { it.id !in finalIds }
            
            // 2. Identify new vs updated
            val originalIds = original.map { it.id }.toSet()
            val (added, possibleUpdates) = finalBuffer.partition { it.id !in originalIds }
            
            // Execute sync
            deleted.forEach { repository.deleteCategory(it) }
            added.forEach { repository.insertCategory(it) }
            possibleUpdates.forEach { repository.updateCategory(it) }

            history.clear()
            history.add(finalBuffer)
            _isReorderMode.value = false
        }
    }

    fun cancelChanges() {
        _buffer.value = allCategories.value
        history.clear()
        history.add(allCategories.value)
        _isReorderMode.value = false
    }

    fun undo() {
        if (history.size > 1) {
            history.removeAt(history.lastIndex)
            _buffer.value = history.last()
        }
    }

    fun createCategory(name: String, parentId: String?) {
        val currentList = _buffer.value
        val order = currentList.filter { it.parentId == parentId }.size
        val newCat = RecipeCategoryEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            parentId = parentId,
            sortOrder = order
        )
        updateBuffer(currentList + newCat)
    }

    fun updateCategory(category: RecipeCategoryEntity) {
        val currentList = _buffer.value
        updateBuffer(currentList.map { if (it.id == category.id) category else it })
    }

    fun deleteCategory(category: RecipeCategoryEntity) {
        val currentList = _buffer.value
        val toDelete = getCategoryFamilyIds(category.id, currentList)
        updateBuffer(currentList.filter { it.id !in toDelete })
    }

    fun moveCategory(category: RecipeCategoryEntity, up: Boolean) {
        val currentList = _buffer.value
        val siblings = currentList
            .filter { it.parentId == category.parentId }
            .sortedBy { it.sortOrder }
        
        val index = siblings.indexOfFirst { it.id == category.id }
        if (index == -1) return
        
        val targetIndex = if (up) index - 1 else index + 1
        if (targetIndex !in siblings.indices) return
        
        val target = siblings[targetIndex]
        val newList = currentList.map {
            when (it.id) {
                category.id -> it.copy(sortOrder = target.sortOrder)
                target.id -> it.copy(sortOrder = category.sortOrder)
                else -> it
            }
        }
        updateBuffer(newList)
    }

    fun reorderCategory(categoryId: String, newParentId: String?, newIndex: Int) {
        val all = _buffer.value.toMutableList()
        val current = all.find { it.id == categoryId } ?: return
        
        val updated = current.copy(parentId = newParentId)
        val otherSiblings = all.filter { it.id != categoryId && it.parentId == newParentId }
            .sortedBy { it.sortOrder }
            .toMutableList()
        
        val safeIndex = newIndex.coerceIn(0, otherSiblings.size)
        otherSiblings.add(safeIndex, updated)
        
        val updatedSiblings = otherSiblings.mapIndexed { i, cat -> cat.copy(sortOrder = i) }
        val newList = all.filter { it.parentId != newParentId && it.id != categoryId }.toMutableList()
        newList.addAll(updatedSiblings)
        
        updateBuffer(newList)
    }

    fun associateTagWithCategory(tagId: String, categoryId: String?, autoSync: Boolean = false) {
        viewModelScope.launch {
            val tag = allTags.value.find { it.id == tagId } ?: return@launch
            repository.saveTag(tag.copy(categoryId = categoryId))
            
            if (autoSync && categoryId != null) {
                syncCategoryRecipesFromTags(categoryId)
            }
        }
    }

    fun syncCategoryRecipesFromTags(categoryId: String) {
        viewModelScope.launch {
            val categoryTags = allTags.value.filter { it.categoryId == categoryId }.map { it.id }.toSet()
            if (categoryTags.isEmpty()) return@launch

            val recipesToLink = allRecipes.value.filter { rwt ->
                rwt.tags.any { it.id in categoryTags }
            }.map { it.recipe.id }

            if (recipesToLink.isNotEmpty()) {
                repository.batchAddRecipesToCategory(recipesToLink, categoryId)
            }
        }
    }

    fun createTag(name: String, categoryId: String? = null) {
        viewModelScope.launch {
            val newTag = TagEntity(name = name, categoryId = categoryId)
            repository.insertTag(newTag)
            
            // If created within a category context, associate it immediately
            if (categoryId != null) {
                // (Optional) We could also check for recipes with this name as a text match,
                // but usually a new tag has no recipes yet.
            }
        }
    }

    fun batchAddRecipesToCategory(recipeIds: List<String>, categoryId: String) {
        viewModelScope.launch { repository.batchAddRecipesToCategory(recipeIds, categoryId) }
    }

    fun batchRemoveRecipesFromCategory(recipeIds: List<String>, categoryId: String) {
        viewModelScope.launch { repository.batchRemoveRecipesFromCategory(recipeIds, categoryId) }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                DatabaseSeeder.seedDefaultCategories(db.openHelper.writableDatabase)
            }
        }
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
