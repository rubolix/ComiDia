package com.rubolix.comidia.ui.screens.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.local.dao.MealPlanDao
import com.rubolix.comidia.data.local.dao.RecipeDao
import com.rubolix.comidia.data.local.dao.StapleDao
import com.rubolix.comidia.data.local.dao.ManualIngredientDao
import com.rubolix.comidia.data.local.entity.UserIngredientPreference
import com.rubolix.comidia.data.local.entity.WeekMetadata
import com.rubolix.comidia.data.local.entity.ShoppingItem
import com.rubolix.comidia.data.repository.MealPlanRepository
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
import javax.inject.Inject

enum class ShoppingSortMode(val label: String) {
    BY_RECIPE("By Recipe"),
    BY_DAY("By Day"),
    BY_AISLE("By Aisle")
}

enum class ShoppingPeriod(val label: String) {
    CURRENT_WEEK("Current Week"),
    NEXT_WEEK("Next Week"),
    TWO_WEEKS("Next 2 Weeks")
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val mealPlanRepository: MealPlanRepository,
    private val recipeDao: RecipeDao,
    private val stapleDao: StapleDao,
    private val manualIngredientDao: ManualIngredientDao
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _currentWeekStart = MutableStateFlow(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    )
    val currentWeekStart: StateFlow<LocalDate> = _currentWeekStart

    private val _sortMode = MutableStateFlow(ShoppingSortMode.BY_AISLE)
    val sortMode: StateFlow<ShoppingSortMode> = _sortMode

    private val _period = MutableStateFlow(ShoppingPeriod.CURRENT_WEEK)
    val period: StateFlow<ShoppingPeriod> = _period

    val weekMetadata: StateFlow<WeekMetadata?> = _currentWeekStart.flatMapLatest { start ->
        mealPlanRepository.getWeekMetadata(start.format(dateFormatter))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val weekSlots = combine(_currentWeekStart, _period) { start, p ->
        val actualStart = if (p == ShoppingPeriod.NEXT_WEEK) start.plusWeeks(1) else start
        val actualEnd = when(p) {
            ShoppingPeriod.CURRENT_WEEK -> start.plusDays(6)
            ShoppingPeriod.NEXT_WEEK -> start.plusWeeks(1).plusDays(6)
            ShoppingPeriod.TWO_WEEKS -> start.plusDays(13)
        }
        actualStart.format(dateFormatter) to actualEnd.format(dateFormatter)
    }.flatMapLatest { (s, e) ->
        mealPlanRepository.getMealSlotsForWeek(s, e)
    }

    private val manualIngredients = combine(_currentWeekStart, _period) { start, p ->
        val actualStart = if (p == ShoppingPeriod.NEXT_WEEK) start.plusWeeks(1) else start
        actualStart
    }.flatMapLatest { s ->
        manualIngredientDao.getManualIngredientsForWeek(s.format(dateFormatter))
    }

    private val ingredientPrefs = _currentWeekStart.flatMapLatest { start ->
        recipeDao.getIngredientPreferences(start.format(dateFormatter))
    }

    private val staples = stapleDao.getAllStaplesIncludingRemoved()

    val shoppingList: StateFlow<Map<String, List<ShoppingItem>>> = combine(
        weekSlots, manualIngredients, ingredientPrefs, staples, _sortMode
    ) { slots, manual, prefs, staplesList, mode ->
        if (slots.isEmpty() && staplesList.isEmpty() && manual.isEmpty()) return@combine emptyMap()

        val prefMap = prefs.associateBy { it.ingredientName.lowercase().trim() }
        val allRawItems = mutableListOf<ShoppingItem>()

        // 1. Process Recipe Ingredients
        data class RecipeSlotInfo(val recipeId: String, val recipeName: String, val date: String, val servings: Int?, val originalServings: Int)
        val recipeSlotInfos = slots.flatMap { slot ->
            slot.recipeRefs.filter { !it.isLeftover && !it.fromFreezer }.mapNotNull { ref ->
                slot.recipes.find { it.id == ref.recipeId }?.let { recipe ->
                    RecipeSlotInfo(recipe.id, recipe.name, slot.mealSlot.date, ref.servings, recipe.servings)
                }
            }
        }
        val allRecipeIds = recipeSlotInfos.map { it.recipeId }.distinct()
        val ingredientsByRecipe = if (allRecipeIds.isNotEmpty()) recipeDao.getIngredientsByRecipeIds(allRecipeIds).groupBy { it.recipeId } else emptyMap()

        recipeSlotInfos.forEach { info ->
            (ingredientsByRecipe[info.recipeId] ?: emptyList()).forEach { ing ->
                if (IngredientUtils.isWater(ing.name)) return@forEach
                val normName = IngredientUtils.normalizeIngredientName(ing.name)
                val pref = prefMap[normName]
                if (pref?.isRemoved == true || pref?.doNotBuy == true) return@forEach
                
                allRawItems.add(ShoppingItem(
                    name = ing.name,
                    quantity = ing.quantity,
                    unit = ing.unit,
                    category = ing.category.ifBlank { "Other" },
                    recipeNames = listOf(info.recipeName),
                    dayLabel = try { LocalDate.parse(info.date).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) } catch(e: Exception) { info.date },
                    needsChecking = pref?.needsChecking ?: false,
                    isPurchased = pref?.isPurchased ?: false
                ))
            }
        }

        // 2. Add Manual Ingredients
        manual.forEach { mi ->
            val normName = IngredientUtils.normalizeIngredientName(mi.name)
            val pref = prefMap[normName]
            if (pref?.isRemoved == true || pref?.doNotBuy == true) return@forEach
            
            allRawItems.add(ShoppingItem(
                name = mi.name,
                quantity = mi.quantity,
                unit = mi.unit,
                category = mi.category.ifBlank { "Other" },
                recipeNames = listOf("Manual"),
                dayLabel = "",
                needsChecking = pref?.needsChecking ?: false,
                isPurchased = pref?.isPurchased ?: false
            ))
        }

        // 3. Add staples
        staplesList.filter { !it.isRemoved && !it.doNotBuy }.forEach { staple ->
            val normName = IngredientUtils.normalizeIngredientName(staple.name)
            allRawItems.add(ShoppingItem(
                name = staple.name,
                quantity = staple.quantity,
                unit = staple.unit,
                category = staple.category.ifBlank { "Staples" },
                recipeNames = listOf("Staple"),
                dayLabel = "",
                needsChecking = staple.needsChecking,
                isPurchased = staple.isPurchased
            ))
        }

        // 4. Smart Grouping
        val groupedList = allRawItems.groupBy { 
            if (mode == ShoppingSortMode.BY_RECIPE) "${it.recipeNames.first()}|${IngredientUtils.normalizeIngredientName(it.name)}"
            else IngredientUtils.normalizeIngredientName(it.name)
        }.map { (_, items) ->
            val first = items.first()
            val normName = IngredientUtils.normalizeIngredientName(first.name)
            val combinedQty = IngredientUtils.combineQuantities(items.map { it.quantity }, items.map { 1.0 }) 

            ShoppingItem(
                name = if (mode == ShoppingSortMode.BY_RECIPE) first.name else normName.replaceFirstChar { it.uppercase() },
                quantity = combinedQty,
                unit = first.unit,
                category = first.category,
                recipeNames = items.flatMap { it.recipeNames }.distinct(),
                dayLabel = items.map { it.dayLabel }.filter { it.isNotBlank() }.distinct().joinToString(", "),
                needsChecking = items.any { it.needsChecking },
                isPurchased = items.any { it.isPurchased }
            )
        }

        val result = mutableMapOf<String, MutableList<ShoppingItem>>()
        val checkNeeded = groupedList.filter { it.needsChecking && !it.isPurchased }
        if (checkNeeded.isNotEmpty()) {
            result["! CHECK AVAILABILITY"] = checkNeeded.toMutableList()
        }

        when (mode) {
            ShoppingSortMode.BY_RECIPE -> {
                groupedList.forEach { item ->
                    val key = item.recipeNames.first()
                    result.getOrPut(key) { mutableListOf() }.add(item)
                }
            }
            ShoppingSortMode.BY_DAY -> {
                groupedList.forEach { item ->
                    val key = item.dayLabel.split(",").first().ifBlank { "Any Day / Staples" }
                    result.getOrPut(key) { mutableListOf() }.add(item)
                }
            }
            ShoppingSortMode.BY_AISLE -> {
                groupedList.forEach { item ->
                    val key = item.category
                    result.getOrPut(key) { mutableListOf() }.add(item)
                }
            }
        }

        result.mapValues { entry -> entry.value.sortedBy { it.name.lowercase() } }.toSortedMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val isShoppingInProgress: StateFlow<Boolean> = shoppingList.map { list ->
        list.values.flatten().any { it.isPurchased }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setSortMode(mode: ShoppingSortMode) { _sortMode.value = mode }
    fun setPeriod(p: ShoppingPeriod) { _period.value = p }

    fun togglePurchased(item: ShoppingItem) {
        viewModelScope.launch {
            val normName = IngredientUtils.normalizeIngredientName(item.name)
            val weekStart = _currentWeekStart.value.format(dateFormatter)
            
            val currentStaples = stapleDao.getAllStaplesIncludingRemoved().first()
            val staple = currentStaples.find { IngredientUtils.normalizeIngredientName(it.name) == normName }
            if (staple != null) {
                stapleDao.updateStaple(staple.copy(isPurchased = !item.isPurchased))
            } else {
                val currentPrefs = recipeDao.getIngredientPreferences(weekStart).first()
                val pref = currentPrefs.find { it.ingredientName == normName }
                recipeDao.insertIngredientPreference((pref ?: UserIngredientPreference(weekStartDate = weekStart, ingredientName = normName))
                    .copy(isPurchased = !item.isPurchased))
            }
        }
    }

    fun commitAvailabilityPass(needToBuyNames: Set<String>, haveItNames: Set<String>) {
        viewModelScope.launch {
            val weekStart = _currentWeekStart.value.format(dateFormatter)
            val currentPrefs = recipeDao.getIngredientPreferences(weekStart).first()
            val currentStaples = stapleDao.getAllStaplesIncludingRemoved().first()

            needToBuyNames.forEach { name ->
                val normalized = IngredientUtils.normalizeIngredientName(name)
                val staple = currentStaples.find { IngredientUtils.normalizeIngredientName(it.name) == normalized }
                if (staple != null) {
                    stapleDao.updateStaple(staple.copy(needsChecking = false, doNotBuy = false))
                } else {
                    val pref = currentPrefs.find { it.ingredientName == normalized }
                    recipeDao.insertIngredientPreference((pref ?: UserIngredientPreference(weekStartDate = weekStart, ingredientName = normalized))
                        .copy(needsChecking = false, doNotBuy = false))
                }
            }

            haveItNames.forEach { name ->
                val normalized = IngredientUtils.normalizeIngredientName(name)
                val staple = currentStaples.find { IngredientUtils.normalizeIngredientName(it.name) == normalized }
                if (staple != null) {
                    stapleDao.updateStaple(staple.copy(needsChecking = false, doNotBuy = true))
                } else {
                    val pref = currentPrefs.find { it.ingredientName == normalized }
                    recipeDao.insertIngredientPreference((pref ?: UserIngredientPreference(weekStartDate = weekStart, ingredientName = normalized))
                        .copy(needsChecking = false, doNotBuy = true))
                }
            }

            mealPlanRepository.saveWeekMetadata(WeekMetadata(weekStart, true))
        }
    }

    fun completeShopping() {
        viewModelScope.launch {
            val weekStart = _currentWeekStart.value.format(dateFormatter)
            val currentPrefs = recipeDao.getIngredientPreferences(weekStart).first()
            val currentStaples = stapleDao.getAllStaplesIncludingRemoved().first()

            currentStaples.filter { it.isPurchased }.forEach { staple ->
                stapleDao.updateStaple(staple.copy(isPurchased = false, doNotBuy = true))
            }

            currentPrefs.filter { it.isPurchased }.forEach { pref ->
                recipeDao.insertIngredientPreference(pref.copy(isPurchased = false, doNotBuy = true))
            }
        }
    }
    
    fun getShareText(): String {
        val list = shoppingList.value
        if (list.isEmpty()) return "Shopping list is empty."
        return buildString {
            append("🛒 TO BUY\n")
            list.forEach { (group, items) ->
                if (group.startsWith("!")) return@forEach
                val toBuy = items.filter { !it.isPurchased }
                if (toBuy.isNotEmpty()) {
                    append("\n[" + group + "]\n")
                    toBuy.forEach { append("• ${if(it.quantity.isNotBlank()) it.quantity + " " else ""}${if(it.unit.isNotBlank()) it.unit + " " else ""}${it.name}\n") }
                }
            }
            
            append("\n✅ PURCHASED\n")
            list.values.flatten().filter { it.isPurchased }.distinctBy { it.name.lowercase() }.forEach { 
                append("• ${it.name}\n")
            }
        }
    }
}
