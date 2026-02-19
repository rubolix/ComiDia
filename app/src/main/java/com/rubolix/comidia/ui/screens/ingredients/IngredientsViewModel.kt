package com.rubolix.comidia.ui.screens.ingredients

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.local.dao.MealPlanDao
import com.rubolix.comidia.data.local.dao.RecipeDao
import com.rubolix.comidia.data.local.dao.ManualIngredientDao
import com.rubolix.comidia.data.local.entity.IngredientEntity
import com.rubolix.comidia.data.local.entity.UserIngredientPreference
import com.rubolix.comidia.data.local.entity.ShoppingItem
import com.rubolix.comidia.data.local.entity.ManualIngredientEntity
import com.rubolix.comidia.util.IngredientUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

enum class IngredientSortMode(val label: String) {
    BY_DAY("By Day"),
    ALPHABETICAL("A-Z"),
    BY_CATEGORY("By Type"),
    BY_RECIPE("By Recipe")
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class IngredientsViewModel @Inject constructor(
    private val mealPlanDao: MealPlanDao,
    private val recipeDao: RecipeDao,
    private val manualIngredientDao: ManualIngredientDao
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _currentWeekStart = MutableStateFlow(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    )
    val currentWeekStart: StateFlow<LocalDate> = _currentWeekStart

    private val _sortMode = MutableStateFlow(IngredientSortMode.BY_CATEGORY)
    val sortMode: StateFlow<IngredientSortMode> = _sortMode

    private val _showPurchased = MutableStateFlow(true)
    val showPurchased: StateFlow<Boolean> = _showPurchased

    private val weekSlots = _currentWeekStart.flatMapLatest { start ->
        val end = start.plusDays(6)
        mealPlanDao.getMealSlotsForDateRange(start.format(dateFormatter), end.format(dateFormatter))
    }

    private val manualIngredients = _currentWeekStart.flatMapLatest { start ->
        manualIngredientDao.getManualIngredientsForWeek(start.format(dateFormatter))
    }

    private val preferences = _currentWeekStart.flatMapLatest { start ->
        recipeDao.getIngredientPreferences(start.format(dateFormatter))
    }

    private val allBaseItems = combine(weekSlots, manualIngredients, _sortMode, preferences) { slots, manual, mode, prefs ->
        val prefMap = prefs.associateBy { it.ingredientName.lowercase().trim() }
        val allRawItems = mutableListOf<ShoppingItem>()

        // 1. Process Recipe Ingredients
        if (slots.isNotEmpty()) {
            data class RecipeSlotInfo(val recipeId: String, val recipeName: String, val date: String, val servings: Int?, val originalServings: Int)
            val recipeSlotInfos = slots.flatMap { slot ->
                slot.recipeRefs.filter { !it.isLeftover && !it.fromFreezer }.mapNotNull { ref ->
                    slot.recipes.find { it.id == ref.recipeId }?.let { recipe ->
                        RecipeSlotInfo(recipe.id, recipe.name, slot.mealSlot.date, ref.servings, recipe.servings)
                    }
                }
            }
            val allRecipeIds = recipeSlotInfos.map { it.recipeId }.distinct()
            if (allRecipeIds.isNotEmpty()) {
                val ingredientsByRecipe = recipeDao.getIngredientsByRecipeIds(allRecipeIds).groupBy { it.recipeId }
                recipeSlotInfos.forEach { info ->
                    val scaleFactor = if (info.servings != null && info.originalServings > 0) info.servings.toDouble() / info.originalServings.toDouble() else 1.0
                    (ingredientsByRecipe[info.recipeId] ?: emptyList()).forEach { ing ->
                        if (IngredientUtils.isWater(ing.name)) return@forEach
                        val normName = IngredientUtils.normalizeIngredientName(ing.name)
                        val pref = prefMap[normName]
                        allRawItems.add(ShoppingItem(
                            name = ing.name,
                            quantity = ing.quantity,
                            unit = ing.unit,
                            category = ing.category.ifBlank { "Other" },
                            recipeNames = listOf(info.recipeName),
                            dayLabel = try { LocalDate.parse(info.date).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) } catch(e: Exception) { info.date },
                            doNotBuy = pref?.doNotBuy ?: false,
                            isRemoved = pref?.isRemoved ?: false,
                            needsChecking = pref?.needsChecking ?: false,
                            isPurchased = pref?.isPurchased ?: false
                        ))
                    }
                }
            }
        }

        // 2. Add Manual Ingredients
        manual.forEach { mi ->
            val normName = IngredientUtils.normalizeIngredientName(mi.name)
            val pref = prefMap[normName]
            allRawItems.add(ShoppingItem(
                name = mi.name,
                quantity = mi.quantity,
                unit = mi.unit,
                category = mi.category.ifBlank { "Other" },
                recipeNames = listOf("Manual"),
                dayLabel = "",
                doNotBuy = pref?.doNotBuy ?: false,
                isRemoved = pref?.isRemoved ?: false,
                needsChecking = pref?.needsChecking ?: false,
                isPurchased = pref?.isPurchased ?: false
            ))
        }

        // 3. Smart Grouping
        allRawItems.groupBy { 
            if (mode == IngredientSortMode.BY_RECIPE) "${it.recipeNames.first()}|${IngredientUtils.normalizeIngredientName(it.name)}"
            else IngredientUtils.normalizeIngredientName(it.name)
        }.map { (_, items) ->
            val first = items.first()
            val normName = IngredientUtils.normalizeIngredientName(first.name)
            val combinedQty = IngredientUtils.combineQuantities(items.map { it.quantity }, items.map { 1.0 }) 

            ShoppingItem(
                name = if (mode == IngredientSortMode.BY_RECIPE) first.name else normName.replaceFirstChar { it.uppercase() },
                quantity = combinedQty,
                unit = first.unit,
                category = if (mode == IngredientSortMode.BY_RECIPE) first.recipeNames.first() else first.category,
                recipeNames = items.flatMap { it.recipeNames }.distinct(),
                dayLabel = items.map { it.dayLabel }.filter { it.isNotBlank() }.distinct().joinToString(", "),
                doNotBuy = items.any { it.doNotBuy },
                isRemoved = items.any { it.isRemoved },
                needsChecking = items.any { it.needsChecking },
                isPurchased = items.any { it.isPurchased }
            )
        }
    }

    val groupedIngredients: StateFlow<Map<String, List<ShoppingItem>>> = combine(allBaseItems, _sortMode, _showPurchased) { items, mode, showPurchased ->
        val activeItems = items.filter { !it.isRemoved }
            .filter { showPurchased || (!it.isPurchased && !it.doNotBuy) }
        
        when (mode) {
            IngredientSortMode.BY_DAY -> {
                val byDay = mutableMapOf<String, MutableList<ShoppingItem>>()
                for (item in activeItems) {
                    val dayKey = item.dayLabel.split(",").first().ifBlank { "Any Day" }
                    byDay.getOrPut(dayKey) { mutableListOf() }.add(item)
                }
                byDay.mapValues { it.value.sortedBy { i -> i.name.lowercase() } }.toSortedMap()
            }
            IngredientSortMode.ALPHABETICAL -> {
                mapOf("All Ingredients" to activeItems.sortedBy { it.name.lowercase() })
            }
            IngredientSortMode.BY_CATEGORY, IngredientSortMode.BY_RECIPE -> {
                activeItems.groupBy { it.category }
                    .toSortedMap()
                    .mapValues { it.value.sortedBy { i -> i.name.lowercase() } }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val removedIngredients: StateFlow<List<ShoppingItem>> = allBaseItems.map { items ->
        items.filter { it.isRemoved }.sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun previousWeek() { _currentWeekStart.update { it.minusWeeks(1) } }
    fun nextWeek() { _currentWeekStart.update { it.plusWeeks(1) } }
    fun setSortMode(mode: IngredientSortMode) { _sortMode.value = mode }
    fun toggleShowPurchased() { _showPurchased.update { !it } }

    private suspend fun updatePref(name: String, block: (UserIngredientPreference) -> UserIngredientPreference) {
        val weekStart = _currentWeekStart.value.format(dateFormatter)
        val normalizedName = IngredientUtils.normalizeIngredientName(name)
        val currentPrefs = preferences.first()
        val currentPref = currentPrefs.find { it.ingredientName == normalizedName }
        
        val newPref = block(currentPref ?: UserIngredientPreference(weekStartDate = weekStart, ingredientName = normalizedName))
        recipeDao.insertIngredientPreference(newPref)
    }

    fun toggleDoNotBuy(item: ShoppingItem) {
        viewModelScope.launch { updatePref(item.name) { it.copy(doNotBuy = !it.doNotBuy) } }
    }

    fun toggleNeedsChecking(item: ShoppingItem) {
        viewModelScope.launch { updatePref(item.name) { it.copy(needsChecking = !it.needsChecking) } }
    }

    fun removeIngredient(item: ShoppingItem) {
        viewModelScope.launch { updatePref(item.name) { it.copy(isRemoved = true) } }
    }

    fun restoreIngredient(item: ShoppingItem) {
        viewModelScope.launch { updatePref(item.name) { it.copy(isRemoved = false) } }
    }

    fun addManualIngredient(name: String, qty: String, unit: String, category: String) {
        viewModelScope.launch {
            manualIngredientDao.insertManualIngredient(ManualIngredientEntity(
                weekStartDate = _currentWeekStart.value.format(dateFormatter),
                name = name,
                quantity = qty,
                unit = unit,
                category = category
            ))
        }
    }

    fun getShareText(): String {
        val ingredients = groupedIngredients.value
        if (ingredients.isEmpty()) return "My ingredients list is empty."
        val weekRange = "${currentWeekStart.value.format(DateTimeFormatter.ofPattern("MMM d"))} - ${
            currentWeekStart.value.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }"
        return buildString {
            append("Ingredients ($weekRange)\n")
            ingredients.forEach { (group, items) ->
                append("\n[$group]\n")
                items.forEach { item ->
                    if (item.doNotBuy) append("~") else append("- ")
                    if (item.needsChecking) append("(? )")
                    if (item.quantity.isNotBlank()) append("${item.quantity} ")
                    if (item.unit.isNotBlank()) append("${item.unit} ")
                    append(item.name)
                    if (item.doNotBuy) append(" (Have it)~")
                    append("\n")
                }
            }
        }
    }
}
