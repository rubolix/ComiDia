@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.rubolix.comidia.ui.screens.mealplan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rubolix.comidia.data.local.entity.*
import com.rubolix.comidia.data.local.dao.MealPlanDao
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

import androidx.compose.foundation.lazy.rememberLazyListState

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
    val sourceLeftovers by viewModel.sourceLeftovers.collectAsState()
    val expandedMealTypes by viewModel.expandedMealTypes.collectAsState()
    val goalStatuses by viewModel.goalStatuses.collectAsState()

    var showRecipePicker by remember { mutableStateOf<Pair<LocalDate, String>?>(null) }
    var showAddWeeklyItem by remember { mutableStateOf(false) }
    var showAddDailyTodo by remember { mutableStateOf<LocalDate?>(null) }
    var showHamburger by remember { mutableStateOf(false) }

    // Detail editing state
    data class RecipeEditContext(val recipe: RecipeEntity, val ref: MealSlotRecipeCrossRef, val slot: MealSlotEntity)
    data class CustomEditContext(val entry: MealSlotCustomEntry, val slot: MealSlotEntity)
    
    var editingRecipeRef by remember { mutableStateOf<RecipeEditContext?>(null) }
    var editingCustomEntry by remember { mutableStateOf<CustomEditContext?>(null) }
    var viewingRecipeId by remember { mutableStateOf<String?>(null) }

    val today = LocalDate.now()
    val listState = rememberLazyListState()

    LaunchedEffect(weekStart) {
        val index = weekDays.indexOfFirst { it == today }
        if (index >= 0) {
            // Scroll to today's day item (index + 2 because of header items)
            listState.animateScrollToItem(index + 2)
        }
    }

    Scaffold { padding ->
        LazyColumn(
            state = listState,
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

            // Whole Week card: compact goal icon + weekly items
            item {
                var showGoalDetails by remember { mutableStateOf(false) }
                val allGoalsMet = goalStatuses.isNotEmpty() && goalStatuses.all { it.isMet }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Whole Week", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                if (goalStatuses.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { showGoalDetails = !showGoalDetails },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            if (allGoalsMet) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = if (allGoalsMet) "All goals met" else "Some goals unmet",
                                            tint = if (allGoalsMet) MaterialTheme.colorScheme.primary
                                                   else MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { showAddWeeklyItem = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Add, "Add weekly item", modifier = Modifier.size(18.dp))
                            }
                        }

                        // Expandable goal details
                        if (showGoalDetails && goalStatuses.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            goalStatuses.forEach { status ->
                                val compLabel = when (status.goal.goalType) {
                                    "eq" -> "Exactly"
                                    "gte", "min" -> "At least"
                                    "lte", "max" -> "At most"
                                    else -> ""
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        if (status.isMet) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (status.isMet) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "$compLabel ${status.goal.targetCount} ${status.goal.description} (${status.currentCount}/${status.goal.targetCount})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (status.isMet) MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // Weekly items
                        if (weeklyItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
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
                    onRemoveCustomEntry = { viewModel.removeCustomEntryFromSlot(it) },
                    onEditRecipe = { recipe, ref, slot -> editingRecipeRef = RecipeEditContext(recipe, ref, slot) },
                    onEditCustom = { entry, slot -> editingCustomEntry = CustomEditContext(entry, slot) },
                    onAddTodo = { showAddDailyTodo = day },
                    onToggleTodo = { viewModel.toggleDailyTodo(it) },
                    onDeleteTodo = { viewModel.deleteDailyTodo(it) }
                )
            }
        }
    }

    // Dialogs
    showRecipePicker?.let { (date, mealType) ->
        val currentSlot = mealSlots.find { it.mealSlot.date == date.toString() && it.mealSlot.mealType == mealType }
        val alreadySelectedIds = currentSlot?.recipes?.map { it.id } ?: emptyList()

        RecipePickerDialog(
            date = date,
            mealType = mealType,
            recipes = recipes,
            sourceLeftovers = sourceLeftovers,
            alreadySelectedIds = alreadySelectedIds,
            onSelect = { recipeId, isConsumption, generatesLeftovers ->
                viewModel.addRecipeToSlot(date, mealType, recipeId, isConsumption, generatesLeftovers)
                showRecipePicker = null
            },
            onSelectCustom = { title, type, isConsumption, generatesLeftovers ->
                viewModel.addCustomEntryToSlot(date, mealType, title, type, isConsumption, generatesLeftovers)
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

    editingRecipeRef?.let { (recipe, ref, slot) ->
        EditItemDetailsDialog(
            title = recipe.name,
            date = slot.date,
            mealType = slot.mealType,
            isConsumption = ref.isLeftover,
            generatesLeftovers = ref.generatesLeftovers,
            fromFreezer = ref.fromFreezer,
            servings = ref.servings ?: recipe.servings,
            isKidApproved = recipe.isKidApproved,
            onTitleClick = { viewingRecipeId = recipe.id },
            onDismiss = { editingRecipeRef = null },
            onSave = { isConsuming, generates, freezer, s, approved ->
                viewModel.updateMealSlotRecipe(ref.copy(isLeftover = isConsuming, generatesLeftovers = generates, fromFreezer = freezer, servings = s))
                if (recipe.isKidApproved != approved) {
                    viewModel.updateRecipeKidApproved(recipe.id, approved)
                }
                editingRecipeRef = null
            }
        )
    }

    editingCustomEntry?.let { (entry, slot) ->
        EditItemDetailsDialog(
            title = entry.title,
            date = slot.date,
            mealType = slot.mealType,
            isConsumption = entry.isLeftover,
            generatesLeftovers = entry.generatesLeftovers,
            fromFreezer = entry.fromFreezer,
            servings = entry.servings ?: 1,
            isKidApproved = false,
            onTitleClick = null,
            onDismiss = { editingCustomEntry = null },
            onSave = { isConsuming, generates, freezer, s, _ ->
                viewModel.updateCustomEntry(entry.copy(isLeftover = isConsuming, generatesLeftovers = generates, fromFreezer = freezer, servings = s))
                editingCustomEntry = null
            }
        )
    }

    viewingRecipeId?.let { id ->
        FullRecipeDialog(
            recipeId = id,
            onDismiss = { viewingRecipeId = null }
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
    onRemoveCustomEntry: (String) -> Unit,
    onEditRecipe: (RecipeEntity, MealSlotRecipeCrossRef, MealSlotEntity) -> Unit,
    onEditCustom: (MealSlotCustomEntry, MealSlotEntity) -> Unit,
    onAddTodo: () -> Unit,
    onToggleTodo: (DailyTodoEntity) -> Unit,
    onDeleteTodo: (DailyTodoEntity) -> Unit
) {
    var showDayMenu by remember { mutableStateOf(false) }
    val allMealTypes = listOf("breakfast", "lunch", "dinner", "snacks", "other")

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
                        allMealTypes.forEach { type ->
                            val isShown = if (type == "dinner") "hidden_dinner" !in extraMealTypes else type in extraMealTypes
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
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Add To-Do") },
                            onClick = { onAddTodo(); showDayMenu = false },
                            leadingIcon = { Icon(Icons.Default.CheckBox, null) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Show visible meal types in order
            val visibleTypes = allMealTypes.filter {
                if (it == "dinner") "hidden_dinner" !in extraMealTypes else it in extraMealTypes
            }

            visibleTypes.forEach { mealType ->
                MealSlotRow(
                    mealType = mealType,
                    slot = daySlots.find { it.mealSlot.mealType == mealType },
                    onAdd = { onAddRecipe(mealType) },
                    onRemoveRecipe = { recipeId -> onRemoveRecipe(mealType, recipeId) },
                    onRemoveCustomEntry = onRemoveCustomEntry,
                    onEditRecipe = onEditRecipe,
                    onEditCustom = onEditCustom
                )
            }

            // Daily todos
            if (dayTodos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
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
    onRemoveRecipe: (String) -> Unit,
    onRemoveCustomEntry: (String) -> Unit,
    onEditRecipe: (RecipeEntity, MealSlotRecipeCrossRef, MealSlotEntity) -> Unit,
    onEditCustom: (MealSlotCustomEntry, MealSlotEntity) -> Unit
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
        if (slot != null && (slot.recipes.isNotEmpty() || slot.customEntries.isNotEmpty())) {
            Column(modifier = Modifier.weight(1f)) {
                // Recipes
                slot.recipes.forEach { recipe ->
                    val ref = slot.recipeRefs.find { it.recipeId == recipe.id } ?: return@forEach
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onEditRecipe(recipe, ref, slot.mealSlot) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                recipe.name,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (ref.generatesLeftovers) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "(Cook & Leftovers)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (ref.isLeftover) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "(Leftovers Only)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (ref.fromFreezer) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.AcUnit, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                            }
                            if (recipe.isKidApproved) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Face, "Kid Approved", modifier = Modifier.size(12.dp), tint = Color(0xFF4CAF50))
                            }
                        }
                        IconButton(
                            onClick = { onRemoveRecipe(recipe.id) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(12.dp))
                        }
                    }
                }
                // Custom Entries
                slot.customEntries.forEach { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onEditCustom(entry, slot.mealSlot) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = when (entry.type) {
                                    "takeout" -> Icons.AutoMirrored.Filled.DirectionsRun
                                    "eating_out" -> Icons.Default.Restaurant
                                    "freezer" -> Icons.Default.AcUnit
                                    "pantry" -> Icons.Default.Kitchen
                                    else -> Icons.Default.EditNote
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                entry.title,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (entry.generatesLeftovers) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("(+ Leftovers)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            if (entry.isLeftover) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("(Leftovers)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            if (entry.fromFreezer) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.AcUnit, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        IconButton(
                            onClick = { onRemoveCustomEntry(entry.id) },
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
    date: LocalDate,
    mealType: String,
    recipes: List<RecipeWithTags>,
    sourceLeftovers: List<MealPlanDao.RecipeWithUsage>,
    alreadySelectedIds: List<String>,
    onSelect: (recipeId: String, isConsumption: Boolean, generatesLeftovers: Boolean) -> Unit,
    onSelectCustom: (title: String, type: String, isConsumption: Boolean, generatesLeftovers: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    // Standard Recipes tab (0) is default
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Logic: Leftovers are available if they were cooked BEFORE the target slot
    val allMealTypes = listOf("breakfast", "lunch", "dinner", "snacks", "other")
    val currentSlotWeight = date.toEpochDay() * 10 + allMealTypes.indexOf(mealType)
    
    val availableLeftovers = sourceLeftovers.filter { usage ->
        val usageDate = LocalDate.parse(usage.lastUsedDate)
        val usageWeight = usageDate.toEpochDay() * 10 + allMealTypes.indexOf(usage.lastUsedMealType)
        usageWeight < currentSlotWeight
    }.distinctBy { it.recipe.id }

    // Flexible Meal state
    var customTitle by remember { mutableStateOf("") }
    var customType by remember { mutableStateOf("takeout") }
    val customTypes = listOf(
        "takeout" to "Takeout",
        "eating_out" to "Eating Out",
        "freezer" to "Freezer",
        "pantry" to "Pantry",
        "other" to "Other"
    )

    val filtered = (if (selectedTab == 1) {
        availableLeftovers.map { RecipeWithTags(it.recipe, emptyList()) }
    } else {
        recipes
    }).filter {
        it.recipe.name.contains(searchQuery, ignoreCase = true) &&
            !it.recipe.isArchived &&
            it.recipe.id !in alreadySelectedIds
    }.distinctBy { it.recipe.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Meal") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                PrimaryTabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Search", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium)
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Leftovers", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium)
                            if (availableLeftovers.isNotEmpty()) {
                                Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp), tint = Color(0xFFFFD700))
                            }
                        }
                    }
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                        Text("Flexible", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab < 2) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search recipes...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                        items(filtered) { rwt ->
                            TextButton(
                                onClick = {
                                    // tab 1 is consumption of leftovers
                                    onSelect(rwt.recipe.id, selectedTab == 1, false)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
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
                    }
                } else {
                    // Flexible Meal Form
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("What are you having?", style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            placeholder = { Text("e.g. Pizza Hut, Pantry pasta") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Type", style = MaterialTheme.typography.labelMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            customTypes.forEach { (id, label) ->
                                FilterChip(
                                    selected = customType == id,
                                    onClick = { customType = id },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onSelectCustom(
                                    customTitle.ifBlank { customTypes.find { it.first == customType }?.second ?: "Meal" },
                                    customType,
                                    false,
                                    false
                                )
                            },
                            modifier = Modifier.align(Alignment.End),
                            enabled = customTitle.isNotBlank() || customType != "other"
                        ) {
                            Text("Add Flexible Meal")
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

@Composable
private fun FullRecipeDialog(
    recipeId: String,
    onDismiss: () -> Unit,
    viewModel: com.rubolix.comidia.ui.screens.recipes.RecipeDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(recipeId) {
        viewModel.setRecipeId(recipeId)
    }
    val recipeFull by viewModel.recipeFull.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(recipeFull?.recipe?.name ?: "Loading...") },
        text = {
            recipeFull?.let { full ->
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (full.ingredients.isNotEmpty()) {
                        Text("Ingredients", style = MaterialTheme.typography.titleSmall)
                        full.ingredients.forEach { ing ->
                            Text("• ${if(ing.quantity.isNotBlank()) ing.quantity + " " else ""}${if(ing.unit.isNotBlank()) ing.unit + " " else ""}${ing.name}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (full.recipe.instructions.isNotBlank()) {
                        Text("Instructions", style = MaterialTheme.typography.titleSmall)
                        Text(full.recipe.instructions, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } ?: run {
                CircularProgressIndicator()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun EditItemDetailsDialog(
    title: String,
    date: String,
    mealType: String,
    isConsumption: Boolean,
    generatesLeftovers: Boolean,
    fromFreezer: Boolean,
    servings: Int,
    isKidApproved: Boolean,
    onTitleClick: (() -> Unit)?,
    onDismiss: () -> Unit,
    onSave: (isConsumption: Boolean, generates: Boolean, freezer: Boolean, servings: Int, approved: Boolean) -> Unit
) {
    var isConsuming by remember { mutableStateOf(isConsumption) }
    var willGenerate by remember { mutableStateOf(generatesLeftovers) }
    var freezer by remember { mutableStateOf(fromFreezer) }
    var servingsCount by remember { mutableIntStateOf(servings) }
    var approved by remember { mutableStateOf(isKidApproved) }

    val formattedDate = try {
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    } catch (e: Exception) {
        date
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = if (onTitleClick != null) Modifier.clickable { onTitleClick() } else Modifier
            ) {
                Text(
                    text = title,
                    textDecoration = if (onTitleClick != null) TextDecoration.Underline else null,
                    color = if (onTitleClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$formattedDate • ${mealType.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Kid approved toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kid Approved", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = approved, onCheckedChange = { approved = it })
                }

                HorizontalDivider()

                // "No ingredients needed" group
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isConsuming || freezer, onCheckedChange = {
                        isConsuming = it
                        if (it) {
                            willGenerate = false
                            freezer = false // Simplify to one "no ingredients" state for UI
                        }
                    })
                    Text("No ingredients needed (leftovers, freezer, pantry, etc.)", style = MaterialTheme.typography.bodyMedium)
                }

                if (!isConsuming && !freezer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = willGenerate, onCheckedChange = { willGenerate = it })
                        Text("Will have leftovers", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Servings inline picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Servings", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (servingsCount > 1) servingsCount-- }) {
                            Icon(Icons.Default.Remove, "Decrease")
                        }
                        Text(
                            text = servingsCount.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { servingsCount++ }) {
                            Icon(Icons.Default.Add, "Increase")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(isConsuming, willGenerate, freezer, servingsCount, approved)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
