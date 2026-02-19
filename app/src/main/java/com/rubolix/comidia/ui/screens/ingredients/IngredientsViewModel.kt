package com.rubolix.comidia.ui.screens.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.local.dao.MealPlanDao
import com.rubolix.comidia.data.local.dao.RecipeDao
import com.rubolix.comidia.data.local.entity.IngredientEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject

data class ShoppingItem(
    val name: String,
    val quantity: String,
    val unit: String,
    val category: String,
    val recipeNames: List<String>,
    val dayLabel: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class IngredientsViewModel @Inject constructor(
    private val mealPlanDao: MealPlanDao,
    private val recipeDao: RecipeDao
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _currentWeekStart = MutableStateFlow(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    )
    val currentWeekStart: StateFlow<LocalDate> = _currentWeekStart

    private val _sortMode = MutableStateFlow(IngredientSortMode.BY_CATEGORY)
    val sortMode: StateFlow<IngredientSortMode> = _sortMode

    private val weekSlots = _currentWeekStart.flatMapLatest { start ->
        val end = start.plusDays(6)
        mealPlanDao.getMealSlotsForDateRange(start.format(dateFormatter), end.format(dateFormatter))
    }

    val groupedIngredients: StateFlow<Map<String, List<ShoppingItem>>> = combine(
        weekSlots, _sortMode
    ) { slots, mode ->
        if (slots.isEmpty()) return@combine emptyMap()

        // Collect recipe IDs with their slot metadata
        data class RecipeSlotInfo(val recipeId: String, val recipeName: String, val date: String)
        val recipeSlotInfos = slots.flatMap { slot ->
            slot.recipes.map { recipe -> RecipeSlotInfo(recipe.id, recipe.name, slot.mealSlot.date) }
        }
        val allRecipeIds = recipeSlotInfos.map { it.recipeId }.distinct()
        if (allRecipeIds.isEmpty()) return@combine emptyMap()

        // Batch query all ingredients
        val allIngredients = recipeDao.getIngredientsByRecipeIds(allRecipeIds)
        val ingredientsByRecipe = allIngredients.groupBy { it.recipeId }

        data class IngredientWithMeta(
            val ingredient: IngredientEntity,
            val recipeName: String,
            val date: String
        )

        val allItems = recipeSlotInfos.flatMap { info ->
            (ingredientsByRecipe[info.recipeId] ?: emptyList()).map { ing ->
                IngredientWithMeta(ing, info.recipeName, info.date)
            }
        }

        // Combine same ingredients
        val combined = allItems.groupBy { it.ingredient.name.lowercase().trim() }
            .map { (_, items) ->
                val first = items.first()
                val totalQty = combineQuantities(items.map { it.ingredient.quantity })
                ShoppingItem(
                    name = first.ingredient.name,
                    quantity = totalQty,
                    unit = first.ingredient.unit,
                    category = first.ingredient.category.ifBlank { "Other" },
                    recipeNames = items.map { it.recipeName }.distinct(),
                    dayLabel = items.map { it.date }.distinct().joinToString(", ") { dateStr ->
                        try {
                            LocalDate.parse(dateStr).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        } catch (e: Exception) { dateStr }
                    }
                )
            }

        when (mode) {
            IngredientSortMode.BY_DAY -> {
                // Group by the first day the ingredient is needed
                val byDay = mutableMapOf<String, MutableList<ShoppingItem>>()
                for (item in combined) {
                    val dayKey = item.dayLabel.split(",").first().trim()
                    byDay.getOrPut(dayKey) { mutableListOf() }.add(item)
                }
                byDay.mapValues { it.value.sortedBy { i -> i.name.lowercase() } }
            }
            IngredientSortMode.ALPHABETICAL -> {
                mapOf("All Ingredients" to combined.sortedBy { it.name.lowercase() })
            }
            IngredientSortMode.BY_CATEGORY -> {
                combined.groupBy { it.category }
                    .toSortedMap()
                    .mapValues { it.value.sortedBy { i -> i.name.lowercase() } }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun previousWeek() { _currentWeekStart.update { it.minusWeeks(1) } }
    fun nextWeek() { _currentWeekStart.update { it.plusWeeks(1) } }
    fun setSortMode(mode: IngredientSortMode) { _sortMode.value = mode }

    private fun combineQuantities(quantities: List<String>): String {
        // Try to sum numeric quantities
        val nums = quantities.mapNotNull { parseFraction(it) }
        return if (nums.size == quantities.size && nums.isNotEmpty()) {
            val sum = nums.sum()
            if (sum == sum.toInt().toDouble()) sum.toInt().toString()
            else String.format("%.1f", sum)
        } else {
            quantities.filter { it.isNotBlank() }.joinToString(" + ")
        }
    }

    private fun parseFraction(s: String): Double? {
        val trimmed = s.trim()
        if (trimmed.isBlank()) return null
        trimmed.toDoubleOrNull()?.let { return it }
        // Handle fractions like "1/2", "1/4"
        val parts = trimmed.split("/")
        if (parts.size == 2) {
            val num = parts[0].trim().toDoubleOrNull() ?: return null
            val den = parts[1].trim().toDoubleOrNull() ?: return null
            if (den != 0.0) return num / den
        }
        return null
    }
}
