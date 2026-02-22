@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.rubolix.comidia.ui.screens.ingredients

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.rubolix.comidia.data.local.entity.ShoppingItem
import com.rubolix.comidia.ui.components.DialogUnderlay
import java.time.format.DateTimeFormatter

@Composable
fun IngredientsScreen(
    onNavigateToShoppingList: () -> Unit,
    onNavigateToStaples: () -> Unit,
    viewModel: IngredientsViewModel = hiltViewModel()
) {
    val weekStart by viewModel.currentWeekStart.collectAsState()
    val groupedIngredients by viewModel.groupedIngredients.collectAsState()
    val removedIngredients by viewModel.removedIngredients.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val showPurchased by viewModel.showPurchased.collectAsState()
    
    var isEditMode by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ingredients") },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Ingredient")
                    }
                    IconButton(onClick = { viewModel.toggleShowPurchased() }) {
                        Icon(
                            if (showPurchased) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Purchased"
                        )
                    }
                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(if (isEditMode) Icons.Default.Check else Icons.Default.Edit, null)
                    }
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, "Sort")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            IngredientSortMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label) },
                                    onClick = { viewModel.setSortMode(mode); showSortMenu = false },
                                    leadingIcon = { if (mode == sortMode) Icon(Icons.Default.Check, null) }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = onNavigateToStaples,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = "Staples")
                }
                FloatingActionButton(
                    onClick = onNavigateToShoppingList,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = "Shopping List")
                }
            }
        }
    ) { padding ->
        val context = LocalContext.current
        
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(padding)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.previousWeek() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous Week")
                        }
                        Text(
                            text = weekStart.format(DateTimeFormatter.ofPattern("MMM d")) + " - " + weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d")),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        IconButton(onClick = { viewModel.nextWeek() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next Week")
                        }
                    }
                }

                if (groupedIngredients.isEmpty() && removedIngredients.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No ingredients for this week.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                        groupedIngredients.forEach { (category, items) ->
                            item {
                                Text(
                                    category,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                                )
                            }
                            items(items, key = { it.name }) { item ->
                                IngredientListItem(
                                    item = item,
                                    isEditMode = isEditMode,
                                    onToggleDoNotBuy = { viewModel.toggleDoNotBuy(item) },
                                    onToggleNeedsChecking = { viewModel.toggleNeedsChecking(item) },
                                    onRemove = { viewModel.removeIngredient(item) }
                                )
                            }
                        }

                        if (isEditMode && removedIngredients.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(24.dp))
                                HorizontalDivider()
                                Text(
                                    "Removed Ingredients",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            items(removedIngredients, key = { "removed|" + it.name }) { item ->
                                RemovedIngredientListItem(item = item, onRestore = { viewModel.restoreIngredient(item) })
                            }
                        }
                    }
                }
            }

            if (showAddDialog) {
                DialogUnderlay(onDismiss = { showAddDialog = false })
            }
        }
    }

    if (showAddDialog) {
        AddIngredientDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, qty, unit, cat -> 
                viewModel.addManualIngredient(name, qty, unit, cat)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun IngredientListItem(
    item: ShoppingItem,
    isEditMode: Boolean,
    onToggleDoNotBuy: () -> Unit,
    onToggleNeedsChecking: () -> Unit,
    onRemove: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> { onToggleDoNotBuy(); false }
                SwipeToDismissBoxValue.EndToStart -> { onToggleNeedsChecking(); false }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> if (item.doNotBuy) MaterialTheme.colorScheme.primary else Color.Gray
                SwipeToDismissBoxValue.EndToStart -> if (item.needsChecking) MaterialTheme.colorScheme.secondary else Color(0xFFFFA500)
                else -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> if (item.doNotBuy) Icons.Default.AddShoppingCart else Icons.Default.Block
                SwipeToDismissBoxValue.EndToStart -> if (item.needsChecking) Icons.Default.Check else Icons.Outlined.QuestionMark
                else -> Icons.Default.Block
            }
            Box(Modifier.fillMaxSize().background(color.copy(alpha = 0.2f)).padding(horizontal = 20.dp), contentAlignment = alignment) {
                Icon(icon, null, tint = color)
            }
        }
    ) {
        var showContextMenu by remember { mutableStateOf(false) }
        Surface(
            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { showContextMenu = true }),
            color = if (item.doNotBuy) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ) {
            Box {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (item.doNotBuy) "✓ " else if (item.needsChecking) "? " else "• ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.doNotBuy) MaterialTheme.colorScheme.primary else if (item.needsChecking) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = buildString {
                                if (item.quantity.isNotBlank()) append("${item.quantity} ")
                                if (item.unit.isNotBlank()) append("${item.unit} ")
                                append(item.name)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (item.doNotBuy) TextDecoration.LineThrough else null,
                            color = if (item.doNotBuy) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        if (item.recipeNames.isNotEmpty()) {
                            Text(item.recipeNames.joinToString(", "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (isEditMode) {
                        IconButton(onClick = onToggleNeedsChecking) {
                            Icon(if (item.needsChecking) Icons.Default.Check else Icons.Outlined.QuestionMark, null, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onToggleDoNotBuy) {
                            Icon(if (item.doNotBuy) Icons.Default.AddShoppingCart else Icons.Default.Block, null, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onRemove) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                DropdownMenu(expanded = showContextMenu, onDismissRequest = { showContextMenu = false }) {
                    DropdownMenuItem(text = { Text("Remove") }, onClick = { onRemove(); showContextMenu = false }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                    DropdownMenuItem(text = { Text(if (item.doNotBuy) "Need it" else "Have it") }, onClick = { onToggleDoNotBuy(); showContextMenu = false }, leadingIcon = { Icon(if (item.doNotBuy) Icons.Default.AddShoppingCart else Icons.Default.Block, null) })
                }
            }
        }
    }
}

@Composable
private fun RemovedIngredientListItem(item: ShoppingItem, onRestore: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, textDecoration = TextDecoration.LineThrough)
            TextButton(onClick = onRestore) { Text("Restore", style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun AddIngredientDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Ingredient") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Qty") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = cat, onValueChange = { cat = it }, label = { Text("Category") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, qty, unit, cat) }, enabled = name.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
