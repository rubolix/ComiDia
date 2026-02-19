package com.rubolix.comidia.ui.screens.mealplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.local.entity.*
import com.rubolix.comidia.data.repository.MealPlanRepository
import com.rubolix.comidia.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MealPlanViewModel @Inject constructor(
    private val mealPlanRepository: MealPlanRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _currentWeekStart = MutableStateFlow(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    )
    val currentWeekStart: StateFlow<LocalDate> = _currentWeekStart

    val weekDays: StateFlow<List<LocalDate>> = _currentWeekStart.map { start ->
        (0..6).map { start.plusDays(it.toLong()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mealSlots: StateFlow<List<MealSlotWithRecipe>> = _currentWeekStart.flatMapLatest { start ->
        val end = start.plusDays(6)
        mealPlanRepository.getMealSlotsForWeek(
            start.format(dateFormatter),
            end.format(dateFormatter)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipes: StateFlow<List<RecipeWithTags>> = recipeRepository.getAllRecipesWithTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun previousWeek() {
        _currentWeekStart.update { it.minusWeeks(1) }
    }

    fun nextWeek() {
        _currentWeekStart.update { it.plusWeeks(1) }
    }

    fun assignRecipe(date: LocalDate, mealType: String, recipeId: String) {
        viewModelScope.launch {
            val slot = MealSlotEntity(
                date = date.format(dateFormatter),
                mealType = mealType,
                recipeId = recipeId
            )
            mealPlanRepository.insertMealSlot(slot)
        }
    }

    fun clearSlot(slot: MealSlotEntity) {
        viewModelScope.launch {
            mealPlanRepository.deleteMealSlot(slot)
        }
    }
}
