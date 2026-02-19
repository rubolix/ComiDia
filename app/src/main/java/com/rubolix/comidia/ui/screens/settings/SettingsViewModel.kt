package com.rubolix.comidia.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.local.entity.MealPlanGoalEntity
import com.rubolix.comidia.data.local.entity.TagEntity
import com.rubolix.comidia.data.repository.GoalRepository
import com.rubolix.comidia.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    val goals: StateFlow<List<MealPlanGoalEntity>> = goalRepository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = recipeRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGoal(goal: MealPlanGoalEntity) {
        viewModelScope.launch { goalRepository.saveGoal(goal) }
    }

    fun deleteGoal(goal: MealPlanGoalEntity) {
        viewModelScope.launch { goalRepository.deleteGoal(goal) }
    }

    fun toggleGoal(goal: MealPlanGoalEntity) {
        viewModelScope.launch { goalRepository.toggleGoal(goal.id, !goal.isActive) }
    }
}
