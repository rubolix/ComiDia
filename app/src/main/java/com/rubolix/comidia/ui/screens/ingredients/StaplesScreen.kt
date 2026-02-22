@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.rubolix.comidia.ui.screens.ingredients

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.rubolix.comidia.data.local.entity.StapleEntity
import com.rubolix.comidia.ui.components.DialogUnderlay

@Composable
fun StaplesScreen(
    onNavigateBack: () -> Unit,
    viewModel: StaplesViewModel = hiltViewModel()
) {
    val staples by viewModel.staples.collectAsState()
    val removedStaples by viewModel.removedStaples.collectAsState()
    var isEditMode by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staples") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(if (isEditMode) Icons.Default.Check else Icons.Default.Edit, null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Staple")
            }
        }
    ) { padding ->
        val grouped = staples.groupBy { it.category.ifBlank { "Uncategorized" } }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                grouped.forEach { (category, items) ->
                    item {
                        Text(category, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    }
                    items(items, key = { it.id }) { staple ->
                        StapleListItem(
                            staple = staple,
                            isEditMode = isEditMode,
                            onToggleDoNotBuy = { viewModel.toggleDoNotBuy(staple) },
                            onToggleNeedsChecking = { viewModel.toggleNeedsChecking(staple) },
                            onRemove = { viewModel.removeStaple(staple) }
                        )
                    }
                }

                if (isEditMode && removedStaples.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider()
                        Text("Removed Staples", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 12.dp))
                    }
                    items(removedStaples, key = { "removed|" + it.id }) { staple ->
                        RemovedStapleListItem(staple = staple, onRestore = { viewModel.restoreStaple(staple) })
                    }
                }
            }

            if (showAddDialog) {
                DialogUnderlay(onDismiss = { showAddDialog = false })
            }
        }
    }

    if (showAddDialog) {
        AddStapleDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, cat -> viewModel.addStaple(name, cat); showAddDialog = false }
        )
    }
}

@Composable
private fun StapleListItem(
    staple: StapleEntity,
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
                SwipeToDismissBoxValue.StartToEnd -> if (staple.doNotBuy) MaterialTheme.colorScheme.primary else Color.Gray
                SwipeToDismissBoxValue.EndToStart -> if (staple.needsChecking) MaterialTheme.colorScheme.secondary else Color(0xFFFFA500)
                else -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> if (staple.doNotBuy) Icons.Default.AddShoppingCart else Icons.Default.Block
                SwipeToDismissBoxValue.EndToStart -> if (staple.needsChecking) Icons.Default.Check else Icons.Outlined.QuestionMark
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
            color = if (staple.doNotBuy) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ) {
            Box {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (staple.doNotBuy) "✓ " else if (staple.needsChecking) "? " else "• ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (staple.doNotBuy) MaterialTheme.colorScheme.primary else if (staple.needsChecking) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        staple.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (staple.doNotBuy) TextDecoration.LineThrough else null,
                        color = if (staple.doNotBuy) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    if (isEditMode) {
                        IconButton(onClick = onToggleNeedsChecking) {
                            Icon(if (staple.needsChecking) Icons.Default.Check else Icons.Outlined.QuestionMark, null, modifier = Modifier.size(20.dp), tint = if (staple.needsChecking) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onToggleDoNotBuy) {
                            Icon(if (staple.doNotBuy) Icons.Default.AddShoppingCart else Icons.Default.Block, null, modifier = Modifier.size(20.dp), tint = if (staple.doNotBuy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onRemove) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                DropdownMenu(expanded = showContextMenu, onDismissRequest = { showContextMenu = false }) {
                    DropdownMenuItem(text = { Text("Remove") }, onClick = { onRemove(); showContextMenu = false }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                    DropdownMenuItem(text = { Text(if (staple.doNotBuy) "Need it" else "Have it") }, onClick = { onToggleDoNotBuy(); showContextMenu = false }, leadingIcon = { Icon(if (staple.doNotBuy) Icons.Default.AddShoppingCart else Icons.Default.Block, null) })
                }
            }
        }
    }
}

@Composable
private fun RemovedStapleListItem(staple: StapleEntity, onRestore: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(staple.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, textDecoration = TextDecoration.LineThrough)
            TextButton(onClick = onRestore) { Text("Restore", style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun AddStapleDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Staple Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item name") }, singleLine = true)
                OutlinedTextField(value = cat, onValueChange = { cat = it }, label = { Text("Category (optional)") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onAdd(name, cat) }, enabled = name.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
