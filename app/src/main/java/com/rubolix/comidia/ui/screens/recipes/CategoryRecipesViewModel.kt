package com.rubolix.comidia.ui.screens.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.local.entity.CategorySmartRuleEntity
import com.rubolix.comidia.data.local.entity.RecipeCategoryEntity
import com.rubolix.comidia.data.local.entity.RecipeWithTagsAndCategories
import com.rubolix.comidia.data.local.entity.TagEntity
import com.rubolix.comidia.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryRecipesState(
    val currentCategory: RecipeCategoryEntity? = null,
    val smartRule: CategorySmartRuleEntity? = null,
    // Map of recipeId to set of categoryIds it belongs to
    val recipeLinks: Map<String, Set<String>> = emptyMap(),
    val allRecipes: List<RecipeWithTagsAndCategories> = emptyList(),
    val allTags: List<TagEntity> = emptyList(),
    val allCategories: List<RecipeCategoryEntity> = emptyList(),
    val isModified: Boolean = false
)

@HiltViewModel
class CategoryRecipesViewModel @Inject constructor(
    private val repository: RecipeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val categoryId: String = checkNotNull(savedStateHandle["categoryId"])

    private val _state = MutableStateFlow(CategoryRecipesState())
    val state: StateFlow<CategoryRecipesState> = _state.asStateFlow()

    private var originalLinks: Map<String, Set<String>> = emptyMap()

    init {
        viewModelScope.launch {
            val cats = repository.getAllCategories().first()
            val current = cats.find { it.id == categoryId }
            val recipes = repository.getAllRecipesWithTagsAndCategories().first()
            val tags = repository.getAllTags().first()
            val rule = repository.getSmartRuleForCategory(categoryId).first()
            
            val links = recipes.associate { it.recipe.id to it.categories.map { c -> c.id }.toSet() }
            originalLinks = links

            _state.update { it.copy(
                currentCategory = current,
                smartRule = rule,
                allCategories = cats,
                allRecipes = recipes,
                allTags = tags,
                recipeLinks = links
            ) }
        }
    }

    fun isRecipeRuleManaged(recipeId: String): Boolean {
        val rule = _state.value.smartRule ?: return false
        val rwt = _state.value.allRecipes.find { it.recipe.id == recipeId } ?: return false
        
        val includeTags = rule.includeTagIds.split(",").filter { it.isNotBlank() }.toSet()
        val includeCats = rule.includeCategoryIds.split(",").filter { it.isNotBlank() }.toSet()
        
        // Simplified recursive check for UI display
        val expandedCats = includeCats.flatMap { cid -> 
            getCategoryFamilyIds(cid, _state.value.allCategories) 
        }.toSet()

        val totalTime = rwt.recipe.prepTimeMinutes + rwt.recipe.cookTimeMinutes
        val matchesMaxTotal = rule.maxTotalTime == null || (totalTime > 0 && totalTime <= rule.maxTotalTime)
        val matchesMinTotal = rule.minTotalTime == null || (totalTime >= rule.minTotalTime)
        val matchesPrep = rule.maxPrepTime == null || (rwt.recipe.prepTimeMinutes > 0 && rwt.recipe.prepTimeMinutes <= rule.maxPrepTime)
        
        // Time matches if (Total matches range) OR (Prep matches Max)
        val timeCriteriaActive = rule.maxTotalTime != null || rule.minTotalTime != null || rule.maxPrepTime != null
        val timeMatches = if (timeCriteriaActive) (matchesMaxTotal && matchesMinTotal) || matchesPrep else true

        val matchesStars = rwt.recipe.rating >= rule.minStars
        val matchesKid = !rule.kidApprovedOnly || rwt.recipe.isKidApproved
        
        val matchesTags = includeTags.isEmpty() || rwt.tags.any { it.id in includeTags }
        val matchesCats = includeCats.isEmpty() || rwt.categories.any { it.id in expandedCats }

        return matchesStars && matchesKid && timeMatches && matchesTags && matchesCats
    }

    private fun getCategoryFamilyIds(parentId: String, allCats: List<RecipeCategoryEntity>): Set<String> {
        val result = mutableSetOf(parentId)
        val children = allCats.filter { it.parentId == parentId }
        children.forEach { child ->
            result.addAll(getCategoryFamilyIds(child.id, allCats))
        }
        return result
    }

    fun addRecipesToCategory(recipeIds: List<String>, targetCategoryId: String) {
        _state.update { current ->
            val newLinks = current.recipeLinks.toMutableMap()
            recipeIds.forEach { rid ->
                val existing = newLinks[rid] ?: emptySet()
                newLinks[rid] = existing + targetCategoryId
            }
            current.copy(recipeLinks = newLinks, isModified = true)
        }
    }

    fun removeRecipeFromCategory(recipeId: String, targetCategoryId: String, removeConflictingTags: Boolean = false) {
        _state.update { current ->
            val newLinks = current.recipeLinks.toMutableMap()
            newLinks[recipeId] = (newLinks[recipeId] ?: emptySet()) - targetCategoryId
            
            // If user opted to remove tags that might re-add it
            if (removeConflictingTags) {
                // Logic to remove tags from this recipe that are linked to this category
                // This would need a 'recipeTagLinks' state buffer too for full consistency
            }
            
            current.copy(recipeLinks = newLinks, isModified = true)
        }
    }

    fun getConflictingTags(recipeId: String, categoryId: String): List<TagEntity> {
        val recipeTags = _state.value.allRecipes.find { it.recipe.id == recipeId }?.tags ?: emptyList()
        return recipeTags.filter { it.categoryId == categoryId }
    }

    fun removeTagsFromRecipe(recipeId: String, tagIds: List<String>) {
        // Since we don't have a session-buffer for tags-on-recipes, 
        // we'll have to handle this during save or immediately.
        // For now, let's keep it simple and focus on the UI flow.
    }

    fun moveRecipe(recipeId: String, fromCategoryId: String, toCategoryId: String) {
        _state.update { current ->
            val newLinks = current.recipeLinks.toMutableMap()
            val existing = newLinks[recipeId] ?: emptySet()
            newLinks[recipeId] = (existing - fromCategoryId) + toCategoryId
            current.copy(recipeLinks = newLinks, isModified = true)
        }
    }

    fun saveChanges(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val currentLinks = _state.value.recipeLinks
            val validCategoryIds = _state.value.allCategories.map { it.id }.toSet()
            
            // Identify what changed globally
            currentLinks.forEach { (recipeId, newCatIds) ->
                val original = originalLinks[recipeId] ?: emptySet()
                if (original != newCatIds) {
                    val recipeFull = repository.getRecipeFull(recipeId).first()
                    recipeFull?.let { full ->
                        // Filter to ensure all category IDs actually exist in the DB
                        val filteredCatIds = newCatIds.filter { it in validCategoryIds }
                        repository.saveRecipe(
                            full.recipe,
                            full.ingredients,
                            full.tags.map { it.id },
                            filteredCatIds
                        )
                    }
                }
            }
            onSuccess()
        }
    }
}
