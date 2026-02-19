package com.rubolix.comidia.ui.screens.mealplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.local.entity.*
import com.rubolix.comidia.data.repository.GoalRepository
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

data class GoalStatus(
    val goal: MealPlanGoalEntity,
    val currentCount: Int,
    val isMet: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MenuViewModel @Inject constructor(
    private val mealPlanRepository: MealPlanRepository,
    private val recipeRepository: RecipeRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _currentWeekStart = MutableStateFlow(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    )
    val currentWeekStart: StateFlow<LocalDate> = _currentWeekStart

    val weekDays: StateFlow<List<LocalDate>> = _currentWeekStart.map { start ->
        (0..6).map { start.plusDays(it.toLong()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Per-day expanded meal types (dinner always shown, others toggled per day)
    private val _expandedMealTypes = MutableStateFlow<Map<LocalDate, Set<String>>>(emptyMap())
    val expandedMealTypes: StateFlow<Map<LocalDate, Set<String>>> = _expandedMealTypes

    val mealSlots: StateFlow<List<MealSlotWithRecipes>> = _currentWeekStart.flatMapLatest { start ->
        val end = start.plusDays(6)
        mealPlanRepository.getMealSlotsForWeek(
            start.format(dateFormatter),
            end.format(dateFormatter)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyItems: StateFlow<List<WeeklyItemEntity>> = _currentWeekStart.flatMapLatest { start ->
        mealPlanRepository.getWeeklyItems(start.format(dateFormatter))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyTodos: StateFlow<List<DailyTodoEntity>> = _currentWeekStart.flatMapLatest { start ->
        val end = start.plusDays(6)
        mealPlanRepository.getDailyTodos(start.format(dateFormatter), end.format(dateFormatter))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipes: StateFlow<List<RecipeWithTags>> = recipeRepository.getAllRecipesWithTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGoals: StateFlow<List<MealPlanGoalEntity>> = goalRepository.getActiveGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goalStatuses: StateFlow<List<GoalStatus>> = combine(
        mealSlots, activeGoals, recipes
    ) { slots, goals, allRecipes ->
        val recipeMap = allRecipes.associate { it.recipe.id to it }
        val weekRecipeIds = slots.flatMap { it.recipes.map { r -> r.id } }.toSet()

        goals.map { goal ->
            val count = weekRecipeIds.count { recipeId ->
                val rwt = recipeMap[recipeId]
                when {
                    goal.tagId != null -> rwt?.tags?.any { it.id == goal.tagId } == true
                    else -> false
                }
            }
            val isMet = when (goal.goalType) {
                "gte" -> count >= goal.targetCount
                "lte" -> count <= goal.targetCount
                "eq" -> count == goal.targetCount
                // Legacy support
                "min" -> count >= goal.targetCount
                "max" -> count <= goal.targetCount
                else -> true
            }
            GoalStatus(goal, count, isMet)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun previousWeek() { _currentWeekStart.update { it.minusWeeks(1) } }
    fun nextWeek() { _currentWeekStart.update { it.plusWeeks(1) } }

    fun toggleMealTypeForDay(day: LocalDate, mealType: String) {
        _expandedMealTypes.update { current ->
            val dayTypes = current[day] ?: emptySet()
            val updated = if (mealType in dayTypes) dayTypes - mealType else dayTypes + mealType
            current + (day to updated)
        }
    }

    fun addRecipeToSlot(date: LocalDate, mealType: String, recipeId: String) {
        viewModelScope.launch {
            mealPlanRepository.addRecipeToSlot(date.format(dateFormatter), mealType, recipeId)
        }
    }

    fun removeRecipeFromSlot(date: LocalDate, mealType: String, recipeId: String) {
        viewModelScope.launch {
            mealPlanRepository.removeRecipeFromSlot(date.format(dateFormatter), mealType, recipeId)
        }
    }

    fun addWeeklyItem(text: String) {
        viewModelScope.launch {
            mealPlanRepository.addWeeklyItem(
                WeeklyItemEntity(
                    weekStartDate = _currentWeekStart.value.format(dateFormatter),
                    text = text
                )
            )
        }
    }

    fun deleteWeeklyItem(item: WeeklyItemEntity) {
        viewModelScope.launch { mealPlanRepository.deleteWeeklyItem(item) }
    }

    fun toggleWeeklyItem(item: WeeklyItemEntity) {
        viewModelScope.launch { mealPlanRepository.toggleWeeklyItem(item.id, !item.isCompleted) }
    }

    fun addDailyTodo(date: LocalDate, text: String) {
        viewModelScope.launch {
            mealPlanRepository.addDailyTodo(
                DailyTodoEntity(date = date.format(dateFormatter), text = text)
            )
        }
    }

    fun deleteDailyTodo(todo: DailyTodoEntity) {
        viewModelScope.launch { mealPlanRepository.deleteDailyTodo(todo) }
    }

    fun toggleDailyTodo(todo: DailyTodoEntity) {
        viewModelScope.launch { mealPlanRepository.toggleDailyTodo(todo.id, !todo.isCompleted) }
    }
}
