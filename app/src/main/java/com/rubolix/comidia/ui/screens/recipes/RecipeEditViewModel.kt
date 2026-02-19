package com.rubolix.comidia.ui.screens.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.local.entity.*
import com.rubolix.comidia.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class IngredientInput(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: String = "",
    val unit: String = "",
    val category: String = ""
)

data class RecipeEditState(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val instructions: String = "",
    val servings: String = "4",
    val prepTimeMinutes: String = "",
    val cookTimeMinutes: String = "",
    val sourceUrl: String = "",
    val rating: Float = 0f,
    val isKidApproved: Boolean = false,
    val notes: String = "",
    val ingredients: List<IngredientInput> = listOf(IngredientInput()),
    val selectedTagIds: Set<String> = emptySet(),
    val isNew: Boolean = true,
    val isSaving: Boolean = false
)

@HiltViewModel
class RecipeEditViewModel @Inject constructor(
    private val repository: RecipeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val recipeId: String? = savedStateHandle.get<String>("recipeId")?.takeIf { it != "new" }

    private val _state = MutableStateFlow(RecipeEditState())
    val state: StateFlow<RecipeEditState> = _state

    val allTags: StateFlow<List<TagEntity>> = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        recipeId?.let { id ->
            viewModelScope.launch {
                repository.getRecipeFull(id).firstOrNull()?.let { full ->
                    _state.value = RecipeEditState(
                        id = full.recipe.id,
                        name = full.recipe.name,
                        instructions = full.recipe.instructions,
                        servings = full.recipe.servings.toString(),
                        prepTimeMinutes = if (full.recipe.prepTimeMinutes > 0) full.recipe.prepTimeMinutes.toString() else "",
                        cookTimeMinutes = if (full.recipe.cookTimeMinutes > 0) full.recipe.cookTimeMinutes.toString() else "",
                        sourceUrl = full.recipe.sourceUrl ?: "",
                        rating = full.recipe.rating,
                        isKidApproved = full.recipe.isKidApproved,
                        notes = full.recipe.notes,
                        ingredients = full.ingredients.map {
                            IngredientInput(it.id, it.name, it.quantity, it.unit, it.category)
                        }.ifEmpty { listOf(IngredientInput()) },
                        selectedTagIds = full.tags.map { it.id }.toSet(),
                        isNew = false
                    )
                }
            }
        }
    }

    fun updateName(name: String) { _state.update { it.copy(name = name) } }
    fun updateInstructions(instructions: String) { _state.update { it.copy(instructions = instructions) } }
    fun updateServings(servings: String) { _state.update { it.copy(servings = servings) } }
    fun updatePrepTime(time: String) { _state.update { it.copy(prepTimeMinutes = time) } }
    fun updateCookTime(time: String) { _state.update { it.copy(cookTimeMinutes = time) } }
    fun updateSourceUrl(url: String) { _state.update { it.copy(sourceUrl = url) } }
    fun updateRating(rating: Float) { _state.update { it.copy(rating = rating) } }
    fun updateKidApproved(approved: Boolean) { _state.update { it.copy(isKidApproved = approved) } }
    fun updateNotes(notes: String) { _state.update { it.copy(notes = notes) } }

    fun updateIngredient(index: Int, ingredient: IngredientInput) {
        _state.update { state ->
            state.copy(ingredients = state.ingredients.toMutableList().apply { set(index, ingredient) })
        }
    }

    fun addIngredient() {
        _state.update { it.copy(ingredients = it.ingredients + IngredientInput()) }
    }

    fun removeIngredient(index: Int) {
        _state.update { state ->
            if (state.ingredients.size <= 1) state
            else state.copy(ingredients = state.ingredients.toMutableList().apply { removeAt(index) })
        }
    }

    fun toggleTag(tagId: String) {
        _state.update { state ->
            state.copy(
                selectedTagIds = if (tagId in state.selectedTagIds)
                    state.selectedTagIds - tagId
                else
                    state.selectedTagIds + tagId
            )
        }
    }

    fun createTag(name: String) {
        viewModelScope.launch {
            val tag = TagEntity(name = name)
            repository.insertTag(tag)
            _state.update { it.copy(selectedTagIds = it.selectedTagIds + tag.id) }
        }
    }

    fun save(onComplete: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) return

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val recipe = RecipeEntity(
                id = s.id,
                name = s.name.trim(),
                instructions = s.instructions.trim(),
                servings = s.servings.toIntOrNull() ?: 4,
                prepTimeMinutes = s.prepTimeMinutes.toIntOrNull() ?: 0,
                cookTimeMinutes = s.cookTimeMinutes.toIntOrNull() ?: 0,
                sourceUrl = s.sourceUrl.trim().ifBlank { null },
                rating = s.rating,
                isKidApproved = s.isKidApproved,
                notes = s.notes.trim(),
                updatedAt = System.currentTimeMillis()
            )
            val ingredients = s.ingredients
                .filter { it.name.isNotBlank() }
                .map { IngredientEntity(it.id, s.id, it.name.trim(), it.quantity.trim(), it.unit.trim(), it.category.trim()) }

            repository.saveRecipe(recipe, ingredients, s.selectedTagIds.toList())
            _state.update { it.copy(isSaving = false) }
            onComplete()
        }
    }
}
