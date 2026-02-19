@file:OptIn(ExperimentalMaterial3Api::class)

package com.rubolix.comidia.ui.screens.mealplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rubolix.comidia.data.local.entity.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MenuScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: MenuViewModel = hiltViewModel()
) {
    val weekStart by viewModel.currentWeekStart.collectAsState()
    val weekDays by viewModel.weekDays.collectAsState()
    val mealSlots by viewModel.mealSlots.collectAsState()
    val weeklyItems by viewModel.weeklyItems.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val visibleMealTypes by viewModel.visibleMealTypes.collectAsState()
    val goalStatuses by viewModel.goalStatuses.collectAsState()

    var showRecipePicker by remember { mutableStateOf<Pair<LocalDate, String>?>(null) }
    var showAddWeeklyItem by remember { mutableStateOf(false) }

    val allMealTypes = listOf("breakfast", "lunch", "dinner", "snacks", "other")
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menu") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Week navigation
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = viewModel::nextWeek) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next week")
                    }
                }
            }

            // Goal compliance
            if (goalStatuses.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (goalStatuses.all { it.isMet })
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Weekly Goals", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            goalStatuses.forEach { status ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        if (status.isMet) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (status.isMet) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "${status.goal.description}: ${status.currentCount}/${status.goal.targetCount} (${status.goal.goalType})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Meal type toggles
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allMealTypes) { type ->
                        FilterChip(
                            selected = type in visibleMealTypes,
                            onClick = { viewModel.toggleMealType(type) },
                            label = { Text(type.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            // Whole-week items
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Whole Week", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showAddWeeklyItem = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Add, "Add weekly item", modifier = Modifier.size(18.dp))
                            }
                        }
                        if (weeklyItems.isEmpty()) {
                            Text(
                                "No weekly items yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        weeklyItems.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = item.isCompleted,
                                    onCheckedChange = { viewModel.toggleWeeklyItem(item) },
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    item.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.deleteWeeklyItem(item) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Days
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

                        visibleMealTypes.sorted().forEach { mealType ->
                            val slot = daySlots.find { it.mealSlot.mealType == mealType }
                            MealSlotRow(
                                mealType = mealType,
                                slot = slot,
                                onAdd = { showRecipePicker = day to mealType },
                                onRemoveRecipe = { recipeId ->
                                    viewModel.removeRecipeFromSlot(day, mealType, recipeId)
                                }
                            )
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
                viewModel.addRecipeToSlot(date, mealType, recipeId)
                showRecipePicker = null
            },
            onDismiss = { showRecipePicker = null }
        )
    }

    // Add weekly item dialog
    if (showAddWeeklyItem) {
        AddWeeklyItemDialog(
            onDismiss = { showAddWeeklyItem = false },
            onAdd = { text ->
                viewModel.addWeeklyItem(text)
                showAddWeeklyItem = false
            }
        )
    }
}

@Composable
private fun MealSlotRow(
    mealType: String,
    slot: MealSlotWithRecipes?,
    onAdd: () -> Unit,
    onRemoveRecipe: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mealType.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(72.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                if (slot != null && slot.recipes.isNotEmpty()) {
                    slot.recipes.forEach { recipe ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssistChip(
                                onClick = {},
                                label = { Text(recipe.name, style = MaterialTheme.typography.bodySmall) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { onRemoveRecipe(recipe.id) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(14.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onAdd, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Add, "Add dish", modifier = Modifier.size(18.dp))
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
    val filtered = recipes.filter {
        it.recipe.name.contains(searchQuery, ignoreCase = true) && !it.recipe.isArchived
    }

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
                    items(filtered) { rwt ->
                        TextButton(
                            onClick = { onSelect(rwt.recipe.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(rwt.recipe.name)
                                if (rwt.tags.isNotEmpty()) {
                                    Text(
                                        rwt.tags.joinToString(", ") { it.name },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                if (recipes.isEmpty()) "No recipes yet" else "No matches",
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddWeeklyItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Weekly Item") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("e.g., Keep fruit bowl stocked") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onAdd(text) }, enabled = text.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
