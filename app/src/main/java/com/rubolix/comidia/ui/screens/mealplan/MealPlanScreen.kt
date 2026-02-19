package com.rubolix.comidia.ui.screens.mealplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rubolix.comidia.data.local.entity.MealSlotWithRecipe
import com.rubolix.comidia.data.local.entity.RecipeWithTags
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanScreen(
    viewModel: MealPlanViewModel = hiltViewModel()
) {
    val weekStart by viewModel.currentWeekStart.collectAsState()
    val weekDays by viewModel.weekDays.collectAsState()
    val mealSlots by viewModel.mealSlots.collectAsState()
    val recipes by viewModel.recipes.collectAsState()

    var showRecipePicker by remember { mutableStateOf<Pair<LocalDate, String>?>(null) }

    val mealTypes = listOf("breakfast", "lunch", "dinner")
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Plan") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Week navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::previousWeek) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous week")
                }
                Text(
                    text = "${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))} — ${
                        weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    }",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = viewModel::nextWeek) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next week")
                }
            }

            // Days
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(weekDays) { day ->
                    val daySlots = mealSlots.filter { it.mealSlot.date == day.toString() }
                    val isToday = day == today

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (isToday) CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) else CardDefaults.cardColors()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${day.format(DateTimeFormatter.ofPattern("M/d"))}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            mealTypes.forEach { mealType ->
                                val slot = daySlots.find { it.mealSlot.mealType == mealType }
                                MealSlotRow(
                                    mealType = mealType,
                                    slot = slot,
                                    onAdd = { showRecipePicker = day to mealType },
                                    onClear = { slot?.let { viewModel.clearSlot(it.mealSlot) } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Recipe picker dialog
    showRecipePicker?.let { (date, mealType) ->
        RecipePickerDialog(
            recipes = recipes,
            onSelect = { recipeId ->
                viewModel.assignRecipe(date, mealType, recipeId)
                showRecipePicker = null
            },
            onDismiss = { showRecipePicker = null }
        )
    }
}

@Composable
private fun MealSlotRow(
    mealType: String,
    slot: MealSlotWithRecipe?,
    onAdd: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = mealType.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        if (slot?.recipe != null) {
            AssistChip(
                onClick = {},
                label = { Text(slot.recipe.name) },
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    IconButton(onClick = onClear, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                    }
                }
            )
        } else {
            OutlinedButton(
                onClick = onAdd,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RecipePickerDialog(
    recipes: List<RecipeWithTags>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = recipes.filter { it.recipe.name.contains(searchQuery, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Recipe") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(filtered) { recipeWithTags ->
                        TextButton(
                            onClick = { onSelect(recipeWithTags.recipe.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                recipeWithTags.recipe.name,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                if (recipes.isEmpty()) "No recipes yet — add some first!"
                                else "No matches",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
