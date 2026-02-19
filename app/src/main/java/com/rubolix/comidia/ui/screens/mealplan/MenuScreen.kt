@file:OptIn(ExperimentalMaterial3Api::class)

package com.rubolix.comidia.ui.screens.mealplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    val dailyTodos by viewModel.dailyTodos.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val expandedMealTypes by viewModel.expandedMealTypes.collectAsState()
    val goalStatuses by viewModel.goalStatuses.collectAsState()

    var showRecipePicker by remember { mutableStateOf<Pair<LocalDate, String>?>(null) }
    var showAddWeeklyItem by remember { mutableStateOf(false) }
    var showAddDailyTodo by remember { mutableStateOf<LocalDate?>(null) }
    var showHamburger by remember { mutableStateOf(false) }

    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menu") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    Box {
                        IconButton(onClick = { showHamburger = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showHamburger,
                            onDismissRequest = { showHamburger = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    showHamburger = false
                                    onNavigateToSettings()
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, null) }
                            )
                        }
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

            // Whole Week card: goals + weekly items only
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

                        // Goal indicators
                        if (goalStatuses.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            goalStatuses.forEach { status ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        if (status.isMet) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (status.isMet) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "${status.goal.description} (${status.currentCount}/${status.goal.targetCount})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (status.isMet) MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Weekly items
                        if (weeklyItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(4.dp))
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

                        if (goalStatuses.isEmpty() && weeklyItems.isEmpty()) {
                            Text(
                                "Set goals in Settings, or add weekly items with +",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Day cards
            items(weekDays) { day ->
                val daySlots = mealSlots.filter { it.mealSlot.date == day.toString() }
                val dayTodos = dailyTodos.filter { it.date == day.toString() }
                val isToday = day == today
                val extraMeals = expandedMealTypes[day] ?: emptySet()

                DayCard(
                    day = day,
                    isToday = isToday,
                    daySlots = daySlots,
                    dayTodos = dayTodos,
                    extraMealTypes = extraMeals,
                    onToggleMealType = { viewModel.toggleMealTypeForDay(day, it) },
                    onAddRecipe = { mealType -> showRecipePicker = day to mealType },
                    onRemoveRecipe = { mealType, recipeId -> viewModel.removeRecipeFromSlot(day, mealType, recipeId) },
                    onAddTodo = { showAddDailyTodo = day },
                    onToggleTodo = { viewModel.toggleDailyTodo(it) },
                    onDeleteTodo = { viewModel.deleteDailyTodo(it) }
                )
            }
        }
    }

    // Dialogs
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

    if (showAddWeeklyItem) {
        TextInputDialog(
            title = "Add Weekly Item",
            placeholder = "e.g., Keep fruit bowl stocked",
            onDismiss = { showAddWeeklyItem = false },
            onConfirm = { viewModel.addWeeklyItem(it); showAddWeeklyItem = false }
        )
    }

    showAddDailyTodo?.let { date ->
        TextInputDialog(
            title = "Add To-Do for ${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.format(DateTimeFormatter.ofPattern("M/d"))}",
            placeholder = "e.g., Defrost chicken",
            onDismiss = { showAddDailyTodo = null },
            onConfirm = { viewModel.addDailyTodo(date, it); showAddDailyTodo = null }
        )
    }
}

@Composable
private fun DayCard(
    day: LocalDate,
    isToday: Boolean,
    daySlots: List<MealSlotWithRecipes>,
    dayTodos: List<DailyTodoEntity>,
    extraMealTypes: Set<String>,
    onToggleMealType: (String) -> Unit,
    onAddRecipe: (String) -> Unit,
    onRemoveRecipe: (String, String) -> Unit,
    onAddTodo: () -> Unit,
    onToggleTodo: (DailyTodoEntity) -> Unit,
    onDeleteTodo: (DailyTodoEntity) -> Unit
) {
    var showDayMenu by remember { mutableStateOf(false) }
    val otherMealTypes = listOf("breakfast", "lunch", "snacks", "other")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isToday) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Day header with 3-dot menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${day.format(DateTimeFormatter.ofPattern("M/d"))}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                )
                Box {
                    IconButton(onClick = { showDayMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, "Day options", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showDayMenu, onDismissRequest = { showDayMenu = false }) {
                        otherMealTypes.forEach { type ->
                            val isShown = type in extraMealTypes
                            DropdownMenuItem(
                                text = { Text(type.replaceFirstChar { it.uppercase() }) },
                                onClick = { onToggleMealType(type); showDayMenu = false },
                                leadingIcon = {
                                    if (isShown) {
                                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Add To-Do") },
                            onClick = { onAddTodo(); showDayMenu = false },
                            leadingIcon = { Icon(Icons.Default.CheckBox, null) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Show expanded meal types that come before dinner
            otherMealTypes.filter { it in extraMealTypes && it in listOf("breakfast", "lunch") }.forEach { mealType ->
                MealSlotRow(
                    mealType = mealType,
                    slot = daySlots.find { it.mealSlot.mealType == mealType },
                    onAdd = { onAddRecipe(mealType) },
                    onRemoveRecipe = { recipeId -> onRemoveRecipe(mealType, recipeId) }
                )
            }

            // Dinner always shown
            MealSlotRow(
                mealType = "dinner",
                slot = daySlots.find { it.mealSlot.mealType == "dinner" },
                onAdd = { onAddRecipe("dinner") },
                onRemoveRecipe = { recipeId -> onRemoveRecipe("dinner", recipeId) }
            )

            // Show expanded meal types after dinner
            otherMealTypes.filter { it in extraMealTypes && it in listOf("snacks", "other") }.forEach { mealType ->
                MealSlotRow(
                    mealType = mealType,
                    slot = daySlots.find { it.mealSlot.mealType == mealType },
                    onAdd = { onAddRecipe(mealType) },
                    onRemoveRecipe = { recipeId -> onRemoveRecipe(mealType, recipeId) }
                )
            }

            // Daily todos
            if (dayTodos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Divider()
                Spacer(modifier = Modifier.height(4.dp))
                dayTodos.forEach { todo ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 1.dp)
                    ) {
                        Checkbox(
                            checked = todo.isCompleted,
                            onCheckedChange = { onToggleTodo(todo) },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            todo.text,
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDeleteTodo(todo) }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealSlotRow(
    mealType: String,
    slot: MealSlotWithRecipes?,
    onAdd: () -> Unit,
    onRemoveRecipe: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = mealType.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(68.dp)
        )
        if (slot != null && slot.recipes.isNotEmpty()) {
            Column(modifier = Modifier.weight(1f)) {
                slot.recipes.forEach { recipe ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            recipe.name,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onRemoveRecipe(recipe.id) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
            IconButton(onClick = onAdd, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Add, "Add another", modifier = Modifier.size(16.dp))
            }
        } else {
            TextButton(
                onClick = onAdd,
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", style = MaterialTheme.typography.labelSmall)
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
private fun TextInputDialog(
    title: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
