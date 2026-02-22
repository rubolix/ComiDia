@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.rubolix.comidia.ui.screens.mealplan

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rubolix.comidia.data.local.entity.*
import com.rubolix.comidia.data.local.dao.MealPlanDao
import com.rubolix.comidia.ui.components.DialogUnderlay
import com.rubolix.comidia.ui.navigation.Screen
import com.rubolix.comidia.ui.screens.recipes.RecipeListScreen
import com.rubolix.comidia.ui.screens.recipes.RecipeListViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MenuScreen(
    navController: NavController,
    onNavigateToRecipeEdit: (String) -> Unit,
    viewModel: MenuViewModel = hiltViewModel()
) {
    val weekStart by viewModel.currentWeekStart.collectAsState()
    val weekDays by viewModel.weekDays.collectAsState()
    val mealSlots by viewModel.mealSlots.collectAsState()
    val weeklyItems by viewModel.weeklyItems.collectAsState()
    val dailyTodos by viewModel.dailyTodos.collectAsState()
    val expandedMealTypes by viewModel.expandedMealTypes.collectAsState()
    val goalStatuses by viewModel.goalStatuses.collectAsState()

    var showRecipePickerByDateType by remember { mutableStateOf<Pair<LocalDate, String>?>(null) }
    var showAddWeeklyItem by remember { mutableStateOf(false) }
    var showAddDailyTodo by remember { mutableStateOf<LocalDate?>(null) }

    data class RecipeEditContext(val recipe: RecipeEntity, val ref: MealSlotRecipeCrossRef, val slot: MealSlotEntity)
    data class CustomEditContext(val entry: MealSlotCustomEntry, val slot: MealSlotEntity)
    
    var editingRecipeRef by remember { mutableStateOf<RecipeEditContext?>(null) }
    var editingCustomEntry by remember { mutableStateOf<CustomEditContext?>(null) }
    var viewingRecipeId by remember { mutableStateOf<String?>(null) }

    // Screen-wide drag state
    var isDraggingActive by remember { mutableStateOf(false) }
    var hoveredSlotKey by remember { mutableStateOf<String?>(null) }
    val slotBounds = remember { mutableStateMapOf<String, Rect>() }
    var deleteZoneBounds by remember { mutableStateOf<Rect?>(null) }
    var isHoveringDelete by remember { mutableStateOf(false) }

    val isAnyDialogOpen = showRecipePickerByDateType != null || showAddWeeklyItem || 
            showAddDailyTodo != null || editingRecipeRef != null || 
            editingCustomEntry != null || viewingRecipeId != null

    val today = LocalDate.now()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun dismissAll() {
        showRecipePickerByDateType = null
        showAddWeeklyItem = false
        showAddDailyTodo = null
        editingRecipeRef = null
        editingCustomEntry = null
        viewingRecipeId = null
    }

    LaunchedEffect(weekStart) {
        val index = weekDays.indexOfFirst { it == today }
        if (index >= 0) {
            listState.animateScrollToItem(index + 2)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Content (Scrollable List)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { event ->
                        event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    },
                    target = remember {
                        object : DragAndDropTarget {
                            override fun onStarted(event: DragAndDropEvent) { isDraggingActive = true }
                            override fun onEnded(event: DragAndDropEvent) {
                                isDraggingActive = false
                                hoveredSlotKey = null
                                isHoveringDelete = false
                            }
                            override fun onMoved(event: DragAndDropEvent) {
                                val dragEvent = event.toAndroidDragEvent()
                                val x = dragEvent.x
                                val y = dragEvent.y
                                val screenOffset = Offset(x, y)
                                
                                // 1. Auto-scroll logic (Larger thresholds for better sensitivity)
                                scope.launch {
                                    if (y < 450) { // Increased threshold for top (scroll up)
                                        listState.scrollBy(-50f)
                                    } else if (y > 1300) { // Scroll down
                                        listState.scrollBy(50f)
                                    }
                                }
                                
                                // 2. Hit testing for Delete Zone
                                val dz = deleteZoneBounds
                                if (dz != null && dz.contains(screenOffset)) {
                                    isHoveringDelete = true
                                    hoveredSlotKey = null
                                } else {
                                    isHoveringDelete = false
                                    // 3. Hit testing for meal slots
                                    var found = false
                                    slotBounds.forEach { (key, rect) ->
                                        if (rect.contains(screenOffset)) {
                                            hoveredSlotKey = key
                                            found = true
                                        }
                                    }
                                    if (!found) hoveredSlotKey = null
                                }
                            }
                            override fun onDrop(event: DragAndDropEvent): Boolean {
                                val data = event.toAndroidDragEvent().clipData.getItemAt(0).text.toString()
                                val parts = data.split("|")
                                if (parts.size < 4) return false
                                
                                val itemType = parts[0]
                                val fromDay = parts[1]
                                val fromMeal = parts[2]
                                val id = parts[3]

                                if (isHoveringDelete) {
                                    if (itemType == "recipe") {
                                        viewModel.removeRecipeFromSlot(LocalDate.parse(fromDay), fromMeal, id)
                                    } else {
                                        viewModel.removeCustomEntryFromSlot(id)
                                    }
                                    isDraggingActive = false
                                    isHoveringDelete = false
                                    return true
                                }

                                val targetKey = hoveredSlotKey ?: return false
                                val targetParts = targetKey.split("|")
                                val toDay = targetParts[0]
                                val toMeal = targetParts[1]
                                
                                if (fromDay == toDay && fromMeal == toMeal) return false
                                
                                if (itemType == "recipe") {
                                    viewModel.moveRecipeBetweenSlots(fromDay, fromMeal, id, toDay, toMeal)
                                } else {
                                    viewModel.moveCustomEntryBetweenSlots(fromDay, fromMeal, id, toDay, toMeal)
                                }
                                isDraggingActive = false
                                return true
                            }
                        }
                    }
                )
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
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

                // Whole Week card
                item {
                    var showGoalDetails by remember { mutableStateOf(false) }
                    val allGoalsMet = goalStatuses.isNotEmpty() && goalStatuses.all { it.isMet }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors()
                    ) {
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
                                        IconButton(onClick = { showGoalDetails = !showGoalDetails }, modifier = Modifier.size(24.dp)) {
                                            Icon(
                                                if (allGoalsMet) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                null,
                                                tint = if (allGoalsMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                TextButton(
                                    onClick = { showAddWeeklyItem = true },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            if (showGoalDetails && goalStatuses.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                goalStatuses.forEach { status ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(status.goal.description, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            "${status.currentCount}/${status.goal.targetCount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (status.isMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }

                            weeklyItems.forEach { item ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = item.isCompleted,
                                        onCheckedChange = { viewModel.toggleWeeklyItem(item) },
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        item.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                                        color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f).clickable { viewModel.toggleWeeklyItem(item) }
                                    )
                                    IconButton(onClick = { viewModel.deleteWeeklyItem(item) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Days
                items(weekDays, key = { it.toString() }) { day ->
                    val daySlots = mealSlots.filter { it.mealSlot.date == day.toString() }
                    val dayTodos = dailyTodos.filter { it.date == day.toString() }
                    DayCard(
                        day = day,
                        isToday = day == today,
                        daySlots = daySlots,
                        dayTodos = dayTodos,
                        extraMealTypes = expandedMealTypes[day] ?: emptySet(),
                        onToggleMealType = { type -> viewModel.toggleMealTypeForDay(day, type) },
                        onAddRecipe = { type -> showRecipePickerByDateType = day to type },
                        onEditRecipe = { r, ref, s -> editingRecipeRef = RecipeEditContext(r, ref, s) },
                        onEditCustom = { e, s -> editingCustomEntry = CustomEditContext(e, s) },
                        onAddTodo = { showAddDailyTodo = day },
                        onToggleTodo = viewModel::toggleDailyTodo,
                        onDeleteTodo = viewModel::deleteDailyTodo,
                        hoveredSlotKey = hoveredSlotKey,
                        onUpdateBounds = { type, rect -> slotBounds["${day}|$type"] = rect }
                    )
                }
            }

            if (isAnyDialogOpen) {
                DialogUnderlay(onDismiss = { dismissAll() })
            }
        }

        // Circular Translucent Delete Zone positioned at bottom right
        if (isDraggingActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 24.dp) // Just above bottom nav area
                    .size(80.dp)
                    .onGloballyPositioned { coords ->
                        val offset = coords.positionInWindow()
                        val size = coords.size
                        deleteZoneBounds = Rect(offset, androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat()))
                    }
                    .background(
                        color = if (isHoveringDelete) Color.Red.copy(alpha = 0.6f) else Color.DarkGray.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }
    }

    // Dialogs (Picker, TextInput, EditDetails, FullRecipe)
    showRecipePickerByDateType?.let { (date, mealType) ->
        Dialog(onDismissRequest = { showRecipePickerByDateType = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize()) {
                val pickerViewModel: RecipeListViewModel = hiltViewModel()
                val alreadySelectedIds = remember(date, mealType, mealSlots) {
                    mealSlots.find { it.mealSlot.date == date.toString() && it.mealSlot.mealType == mealType }?.recipes?.map { it.id }?.toSet() ?: emptySet()
                }
                LaunchedEffect(date, mealType, alreadySelectedIds) { pickerViewModel.setPickerMode(date.toString(), mealType, alreadySelectedIds) }
                DisposableEffect(Unit) { onDispose { pickerViewModel.setPickerMode(null, null) } }
                RecipeListScreen(onNavigateBack = { showRecipePickerByDateType = null }, onNavigateToRecipe = { recipeId: String -> viewModel.addRecipeToSlot(date, mealType, recipeId, false, false); showRecipePickerByDateType = null }, onNavigateToNewRecipe = { _: String? -> onNavigateToRecipeEdit("new") }, onNavigateToManageCategories = { }, onSelectLeftover = { _, _, rid -> viewModel.addRecipeToSlot(date, mealType, rid, true, false); showRecipePickerByDateType = null }, onSelectFlexible = { _, _, title, customType -> viewModel.addCustomEntryToSlot(date, mealType, title, customType ?: "other", false, false); showRecipePickerByDateType = null }, viewModel = pickerViewModel)
            }
        }
    }
    if (showAddWeeklyItem) TextInputDialog(title = "Add Weekly Item", placeholder = "e.g., Stock fruit bowl", onDismiss = { showAddWeeklyItem = false }, onConfirm = { viewModel.addWeeklyItem(it); showAddWeeklyItem = false })
    showAddDailyTodo?.let { date -> TextInputDialog(title = "Add To-Do", placeholder = "e.g., Defrost chicken", onDismiss = { showAddDailyTodo = null }, onConfirm = { viewModel.addDailyTodo(date, it); showAddDailyTodo = null }) }
    editingRecipeRef?.let { (recipe, ref, slot) -> EditItemDetailsDialog(title = recipe.name, date = slot.date, mealType = slot.mealType, isConsumption = ref.isLeftover, generatesLeftovers = ref.generatesLeftovers, fromFreezer = ref.fromFreezer, servings = ref.servings ?: recipe.servings, recipe = recipe, onTitleClick = { viewingRecipeId = recipe.id }, onDismiss = { editingRecipeRef = null }, onSave = { c, g, f, s -> viewModel.updateMealSlotRecipe(ref.copy(isLeftover = c, generatesLeftovers = g, fromFreezer = f, servings = s)); editingRecipeRef = null }) }
    editingCustomEntry?.let { (entry, slot) -> EditItemDetailsDialog(title = entry.title, date = slot.date, mealType = slot.mealType, isConsumption = entry.isLeftover, generatesLeftovers = entry.generatesLeftovers, fromFreezer = entry.fromFreezer, servings = entry.servings ?: 1, recipe = null, onTitleClick = null, onDismiss = { editingCustomEntry = null }, onSave = { c, g, f, s -> viewModel.updateCustomEntry(entry.copy(isLeftover = c, generatesLeftovers = g, fromFreezer = f, servings = s)); editingCustomEntry = null }) }
    viewingRecipeId?.let { id -> FullRecipeDialog(recipeId = id, onDismiss = { viewingRecipeId = null }, onEdit = { viewingRecipeId = null; onNavigateToRecipeEdit(id) }) }
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
    onEditRecipe: (RecipeEntity, MealSlotRecipeCrossRef, MealSlotEntity) -> Unit,
    onEditCustom: (MealSlotCustomEntry, MealSlotEntity) -> Unit,
    onAddTodo: () -> Unit,
    onToggleTodo: (DailyTodoEntity) -> Unit,
    onDeleteTodo: (DailyTodoEntity) -> Unit,
    hoveredSlotKey: String?,
    onUpdateBounds: (String, Rect) -> Unit
) {
    var showDayMenu by remember { mutableStateOf(false) }
    val allMealTypes = listOf("breakfast", "lunch", "dinner", "snacks", "other")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${day.format(DateTimeFormatter.ofPattern("M/d"))}", style = MaterialTheme.typography.titleSmall, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium)
                IconButton(onClick = { showDayMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(18.dp)) }
                DropdownMenu(expanded = showDayMenu, onDismissRequest = { showDayMenu = false }) {
                    allMealTypes.forEach { type ->
                        val isShown = if (type == "dinner") "hidden_dinner" !in extraMealTypes else type in extraMealTypes
                        DropdownMenuItem(text = { Text(type.replaceFirstChar { it.uppercase() }) }, onClick = { onToggleMealType(type); showDayMenu = false }, leadingIcon = { if (isShown) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) })
                    }
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Add To-Do") }, onClick = { onAddTodo(); showDayMenu = false }, leadingIcon = { Icon(Icons.Default.CheckBox, null) })
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            
            // Refined Visibility: Show if explicitly toggled OR if it has recipes
            val visibleTypes = allMealTypes.filter { type ->
                val isDinner = type == "dinner"
                val isExplicitlyShown = if (isDinner) "hidden_dinner" !in extraMealTypes else type in extraMealTypes
                val hasContent = daySlots.any { it.mealSlot.mealType == type && (it.recipes.isNotEmpty() || it.customEntries.isNotEmpty()) }
                isExplicitlyShown || hasContent
            }

            visibleTypes.forEach { type ->
                val slot = daySlots.find { it.mealSlot.mealType == type }
                MealTypeSection(
                    day = day.toString(),
                    type = type,
                    slot = slot,
                    isHovered = hoveredSlotKey == "${day}|$type",
                    onAdd = { onAddRecipe(type) },
                    onEditRecipe = { r, ref -> onEditRecipe(r, ref, slot!!.mealSlot) },
                    onEditCustom = { e -> onEditCustom(e, slot!!.mealSlot) },
                    onUpdateBounds = { onUpdateBounds(type, it) }
                )
            }
            if (dayTodos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                dayTodos.forEach { todo ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = todo.isCompleted, onCheckedChange = { onToggleTodo(todo) }, modifier = Modifier.size(32.dp))
                        Text(todo.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null)
                        IconButton(onClick = { onDeleteTodo(todo) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MealTypeSection(
    day: String,
    type: String,
    slot: MealSlotWithRecipes?,
    isHovered: Boolean,
    onAdd: () -> Unit,
    onEditRecipe: (RecipeEntity, MealSlotRecipeCrossRef) -> Unit,
    onEditCustom: (MealSlotCustomEntry) -> Unit,
    onUpdateBounds: (Rect) -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val offset = coords.positionInWindow()
                val size = coords.size
                onUpdateBounds(Rect(offset, androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat())))
            },
        color = if (isHovered) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.medium,
        border = if (isHovered) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = type.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            if (slot != null && (slot.recipes.isNotEmpty() || slot.customEntries.isNotEmpty())) {
                slot.recipes.forEach { recipe ->
                    val ref = slot.recipeRefs.find { it.recipeId == recipe.id }!!
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().dragAndDropSource {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { startTransfer(DragAndDropTransferData(ClipData.newPlainText("meal_item", "recipe|$day|$type|${recipe.id}"))) },
                                onDrag = { change, _ -> change.consume() }
                            )
                        }.clickable { onEditRecipe(recipe, ref) }.padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.DragIndicator, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Spacer(Modifier.width(8.dp))
                        Text(recipe.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
                slot.customEntries.forEach { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().dragAndDropSource {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { startTransfer(DragAndDropTransferData(ClipData.newPlainText("meal_item", "custom|$day|$type|${entry.id}"))) },
                                onDrag = { change, _ -> change.consume() }
                            )
                        }.clickable { onEditCustom(entry) }.padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.DragIndicator, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Spacer(Modifier.width(8.dp))
                        Text(entry.title, style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(
                    onClick = onAdd, 
                    modifier = Modifier.height(32.dp).align(Alignment.End),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                IconButton(
                    onClick = onAdd, 
                    modifier = Modifier.size(32.dp).align(Alignment.End)
                ) { 
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp)) 
                }
            }
        }
    }
}

@Composable
private fun TextInputDialog(title: String, placeholder: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(placeholder) }, singleLine = true) }, confirmButton = { TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Add") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun FullRecipeDialog(recipeId: String, onDismiss: () -> Unit, onEdit: () -> Unit, viewModel: com.rubolix.comidia.ui.screens.recipes.RecipeDetailViewModel = hiltViewModel()) {
    LaunchedEffect(recipeId) { viewModel.setRecipeId(recipeId) }
    val recipeFull by viewModel.recipeFull.collectAsState()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(recipeFull?.recipe?.name ?: "Loading...") }, text = { recipeFull?.let { full -> Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Row(verticalAlignment = Alignment.CenterVertically) { repeat(5) { i -> Icon(if (i < full.recipe.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }; if (full.recipe.isKidApproved) { Spacer(Modifier.width(8.dp)); Icon(Icons.Default.Face, null, modifier = Modifier.size(16.dp), tint = Color(0xFF4CAF50)) } }; Column(horizontalAlignment = Alignment.End) { if (full.recipe.totalTimeMinutes > 0) Text("Total: ${full.recipe.totalTimeMinutes}m", style = MaterialTheme.typography.labelSmall); if (full.recipe.prepTimeMinutes > 0) Text("Prep: ${full.recipe.prepTimeMinutes}m", style = MaterialTheme.typography.labelSmall) } }
    HorizontalDivider(); if (full.ingredients.isNotEmpty()) { Text("Ingredients", style = MaterialTheme.typography.titleSmall); full.ingredients.forEach { ing -> Text("• ${if(ing.quantity.isNotBlank()) ing.quantity + " " else ""}${if(ing.unit.isNotBlank()) ing.unit + " " else ""}${ing.name}", style = MaterialTheme.typography.bodySmall) } }; if (full.recipe.instructions.isNotBlank()) { Text("Instructions", style = MaterialTheme.typography.titleSmall); Text(full.recipe.instructions, style = MaterialTheme.typography.bodySmall) } } } ?: CircularProgressIndicator() }, confirmButton = { Row { if (recipeFull != null) { TextButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Edit") } }
    TextButton(onClick = onDismiss) { Text("Close") } } })
}

@Composable
private fun EditItemDetailsDialog(title: String, date: String, mealType: String, isConsumption: Boolean, generatesLeftovers: Boolean, fromFreezer: Boolean, servings: Int, recipe: RecipeEntity?, onTitleClick: (() -> Unit)?, onDismiss: () -> Unit, onSave: (isConsumption: Boolean, generates: Boolean, freezer: Boolean, servings: Int) -> Unit) {
    var isConsuming by remember { mutableStateOf(isConsumption) }
    var willGenerate by remember { mutableStateOf(generatesLeftovers) }
    var freezer by remember { mutableStateOf(fromFreezer) }
    var servingsCount by remember { mutableIntStateOf(servings) }
    val formattedDate = try { LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE, MMM d")) } catch (e: Exception) { date }
    AlertDialog(onDismissRequest = onDismiss, title = { Column(modifier = if (onTitleClick != null) Modifier.clickable { onTitleClick() } else Modifier) { Text(text = title, textDecoration = if (onTitleClick != null) TextDecoration.Underline else null, color = if (onTitleClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface); Text(text = "$formattedDate • ${mealType.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { recipe?.let { r -> Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Row(verticalAlignment = Alignment.CenterVertically) { repeat(5) { i -> Icon(if (i < r.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }; if (r.isKidApproved) { Spacer(Modifier.width(8.dp)); Icon(Icons.Default.Face, null, modifier = Modifier.size(16.dp), tint = Color(0xFF4CAF50)) } }; Column(horizontalAlignment = Alignment.End) { if (r.totalTimeMinutes > 0) Text("Total: ${r.totalTimeMinutes}m", style = MaterialTheme.typography.labelSmall); if (r.prepTimeMinutes > 0) Text("Prep: ${r.prepTimeMinutes}m", style = MaterialTheme.typography.labelSmall) } }; HorizontalDivider() }
    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = isConsuming || freezer, onCheckedChange = { isConsuming = it; if (it) { willGenerate = false; freezer = false } }); Text("No ingredients needed (leftovers, freezer, etc.)", style = MaterialTheme.typography.bodyMedium) }
    if (!isConsuming && !freezer) { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = willGenerate, onCheckedChange = { willGenerate = it }); Text("Will have leftovers", style = MaterialTheme.typography.bodyMedium) } }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Servings", style = MaterialTheme.typography.bodyMedium); Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { if (servingsCount > 1) servingsCount-- }) { Icon(Icons.Default.Remove, null) }; Text(text = servingsCount.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 8.dp)); IconButton(onClick = { servingsCount++ }) { Icon(Icons.Default.Add, null) } } } } }, confirmButton = { TextButton(onClick = { onSave(isConsuming, willGenerate, freezer, servingsCount) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeleteZone(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    onDelete: (String) -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    AnimatedVisibility(visible = isVisible, enter = expandIn(expandFrom = Alignment.Center) + fadeIn(), exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut(), modifier = modifier) {
        Surface(
            color = if (isHovered) Color.Red.copy(alpha = 0.6f) else Color.DarkGray.copy(alpha = 0.2f),
            shape = CircleShape,
            tonalElevation = 8.dp,
            modifier = Modifier
                .size(80.dp)
                .onGloballyPositioned { /* Bounds tracked by parent moved check if needed, but here simple target works */ }
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { it.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN) },
                    target = remember { object : DragAndDropTarget {
                        override fun onEntered(event: DragAndDropEvent) { isHovered = true }
                        override fun onExited(event: DragAndDropEvent) { isHovered = false }
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            isHovered = false
                            onDelete(event.toAndroidDragEvent().clipData.getItemAt(0).text.toString())
                            return true
                        }
                    } }
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(36.dp), tint = Color.White)
            }
        }
    }
}
