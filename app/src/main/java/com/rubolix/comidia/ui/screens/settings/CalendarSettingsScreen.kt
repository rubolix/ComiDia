@file:OptIn(ExperimentalMaterial3Api::class)

package com.rubolix.comidia.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rubolix.comidia.data.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalendarSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val firstDayOfWeek = settingsRepository.getFirstDayOfWeek()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "monday")

    val defaultMealTypes = settingsRepository.getDefaultMealTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf("dinner"))

    fun setFirstDayOfWeek(day: String) {
        viewModelScope.launch { settingsRepository.setFirstDayOfWeek(day) }
    }

    fun toggleMealType(type: String) {
        viewModelScope.launch {
            val current = defaultMealTypes.value
            val updated = if (type in current) current - type else current + type
            if (updated.isNotEmpty()) {
                settingsRepository.setDefaultMealTypes(updated)
            }
        }
    }
}

@Composable
fun CalendarSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: CalendarSettingsViewModel = hiltViewModel()
) {
    val firstDay by viewModel.firstDayOfWeek.collectAsState()
    val mealTypes by viewModel.defaultMealTypes.collectAsState()

    val allMealTypes = listOf("breakfast", "lunch", "dinner", "snacks", "other")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // First day of week
            Text("First Day of Week", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("monday", "sunday").forEach { day ->
                    FilterChip(
                        selected = firstDay == day,
                        onClick = { viewModel.setFirstDayOfWeek(day) },
                        label = { Text(day.replaceFirstChar { it.uppercase() }) },
                        leadingIcon = if (firstDay == day) {
                            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }

            Divider()

            // Default visible meals
            Text("Default Visible Meals", style = MaterialTheme.typography.titleMedium)
            Text(
                "These meal types will be shown by default on each day",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            allMealTypes.forEach { type ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = type in mealTypes,
                        onCheckedChange = { viewModel.toggleMealType(type) }
                    )
                    Text(
                        type.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
