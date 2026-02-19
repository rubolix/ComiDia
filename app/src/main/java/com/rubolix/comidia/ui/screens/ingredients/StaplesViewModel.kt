package com.rubolix.comidia.ui.screens.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.local.entity.StapleEntity
import com.rubolix.comidia.data.repository.StapleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StaplesViewModel @Inject constructor(
    private val repository: StapleRepository
) : ViewModel() {

    val staples: StateFlow<List<StapleEntity>> = repository.getAllStaples()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val removedStaples: StateFlow<List<StapleEntity>> = repository.getAllStaplesIncludingRemoved()
        .map { list -> list.filter { it.isRemoved } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addStaple(name: String, category: String = "") {
        viewModelScope.launch {
            repository.saveStaple(StapleEntity(name = name, category = category))
        }
    }

    fun toggleDoNotBuy(staple: StapleEntity) {
        viewModelScope.launch {
            repository.updateStaple(staple.copy(doNotBuy = !staple.doNotBuy))
        }
    }

    fun toggleNeedsChecking(staple: StapleEntity) {
        viewModelScope.launch {
            repository.updateStaple(staple.copy(needsChecking = !staple.needsChecking))
        }
    }

    fun removeStaple(staple: StapleEntity) {
        viewModelScope.launch {
            repository.markAsRemoved(staple.id)
        }
    }

    fun restoreStaple(staple: StapleEntity) {
        viewModelScope.launch {
            repository.restoreStaple(staple.id)
        }
    }
}
