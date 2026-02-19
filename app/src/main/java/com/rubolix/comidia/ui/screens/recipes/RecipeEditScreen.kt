package com.rubolix.comidia.ui.screens.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecipeEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    var showNewTagDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New Recipe" else "Edit Recipe") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save(onNavigateBack) },
                        enabled = state.name.isNotBlank() && !state.isSaving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Recipe name
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("Recipe name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Times & servings row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.prepTimeMinutes,
                        onValueChange = viewModel::updatePrepTime,
                        label = { Text("Prep (min)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = state.cookTimeMinutes,
                        onValueChange = viewModel::updateCookTime,
                        label = { Text("Cook (min)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = state.servings,
                        onValueChange = viewModel::updateServings,
                        label = { Text("Servings") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            // Tags
            item {
                Text("Tags", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allTags) { tag ->
                        FilterChip(
                            selected = tag.id in state.selectedTagIds,
                            onClick = { viewModel.toggleTag(tag.id) },
                            label = { Text(tag.name) }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { showNewTagDialog = true },
                            label = { Text("+ New tag") }
                        )
                    }
                }
            }

            // Star rating
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Rating", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            repeat(5) { i ->
                                IconButton(onClick = {
                                    viewModel.updateRating(if (state.rating == (i + 1).toFloat()) 0f else (i + 1).toFloat())
                                }, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        if (i < state.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Rate ${i + 1}",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Kid Approved", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        IconToggleButton(
                            checked = state.isKidApproved,
                            onCheckedChange = viewModel::updateKidApproved
                        ) {
                            Icon(
                                imageVector = if (state.isKidApproved) Icons.Default.Face else Icons.Default.Face,
                                contentDescription = "Kid Approved",
                                tint = if (state.isKidApproved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // Source URL
            item {
                OutlinedTextField(
                    value = state.sourceUrl,
                    onValueChange = viewModel::updateSourceUrl,
                    label = { Text("Source URL (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Ingredients section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ingredients", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::addIngredient) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }

            itemsIndexed(state.ingredients) { index, ingredient ->
                IngredientRow(
                    ingredient = ingredient,
                    onUpdate = { viewModel.updateIngredient(index, it) },
                    onRemove = { viewModel.removeIngredient(index) },
                    canRemove = state.ingredients.size > 1
                )
            }

            // Instructions
            item {
                OutlinedTextField(
                    value = state.instructions,
                    onValueChange = viewModel::updateInstructions,
                    label = { Text("Instructions") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp),
                    maxLines = 10
                )
            }

            // Notes
            item {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::updateNotes,
                    label = { Text("Notes (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    maxLines = 5
                )
            }
        }
    }

    if (showNewTagDialog) {
        NewTagDialog(
            onDismiss = { showNewTagDialog = false },
            onCreate = { name ->
                viewModel.createTag(name)
                showNewTagDialog = false
            }
        )
    }
}

@Composable
private fun IngredientRow(
    ingredient: IngredientInput,
    onUpdate: (IngredientInput) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = ingredient.name,
            onValueChange = { onUpdate(ingredient.copy(name = it)) },
            label = { Text("Ingredient") },
            modifier = Modifier.weight(2f),
            singleLine = true
        )
        OutlinedTextField(
            value = ingredient.quantity,
            onValueChange = { onUpdate(ingredient.copy(quantity = it)) },
            label = { Text("Qty") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = ingredient.unit,
            onValueChange = { onUpdate(ingredient.copy(unit = it)) },
            label = { Text("Unit") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove")
            }
        }
    }
}

@Composable
private fun NewTagDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var tagName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Tag") },
        text = {
            OutlinedTextField(
                value = tagName,
                onValueChange = { tagName = it },
                label = { Text("Tag name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(tagName) },
                enabled = tagName.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
