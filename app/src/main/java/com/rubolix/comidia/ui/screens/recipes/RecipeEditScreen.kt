@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.rubolix.comidia.ui.screens.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rubolix.comidia.data.local.entity.TagEntity
import com.rubolix.comidia.data.local.entity.RecipeCategoryEntity
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.rubolix.comidia.ui.components.DialogUnderlay
import com.rubolix.comidia.ui.components.CategoryNode
import com.rubolix.comidia.ui.components.CategoryTreeUtils

@Composable
fun RecipeEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecipeEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val frequentTags by viewModel.mostFrequentTags.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New Recipe" else "Edit Recipe") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(onClick = { viewModel.save(onNavigateBack) }) {
                            Icon(Icons.Default.Save, "Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("Recipe Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Hierarchical Categories Section
            CategorySelectionSection(
                selectedCategoryIds = state.selectedCategoryIds,
                allCategories = allCategories,
                onToggleCategory = viewModel::toggleCategory
            )

            // Tags Section
            TagManagementSection(
                selectedTagIds = state.selectedTagIds,
                allTags = allTags,
                frequentTags = frequentTags,
                onToggleTag = viewModel::toggleTag,
                onCreateTag = viewModel::createTag
            )

            // Ingredients
            Text("Ingredients", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            state.ingredients.forEachIndexed { index, ing ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = ing.quantity,
                        onValueChange = { viewModel.updateIngredient(index, ing.copy(quantity = it)) },
                        label = { Text("Qty") },
                        modifier = Modifier.weight(0.2f)
                    )
                    OutlinedTextField(
                        value = ing.unit,
                        onValueChange = { viewModel.updateIngredient(index, ing.copy(unit = it)) },
                        label = { Text("Unit") },
                        modifier = Modifier.weight(0.3f)
                    )
                    OutlinedTextField(
                        value = ing.name,
                        onValueChange = { viewModel.updateIngredient(index, ing.copy(name = it)) },
                        label = { Text("Name") },
                        modifier = Modifier.weight(0.5f)
                    )
                    IconButton(onClick = { viewModel.removeIngredient(index) }) {
                        Icon(Icons.Default.RemoveCircleOutline, "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            TextButton(onClick = viewModel::addIngredient) {
                Icon(Icons.Default.Add, null)
                Text("Add Ingredient")
            }

            // Instructions
            OutlinedTextField(
                value = state.instructions,
                onValueChange = viewModel::updateInstructions,
                label = { Text("Instructions") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
            )

            // Times & Servings
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.prepTimeMinutes,
                    onValueChange = viewModel::updatePrepTime,
                    label = { Text("Prep (min)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.cookTimeMinutes,
                    onValueChange = viewModel::updateCookTime,
                    label = { Text("Cook (min)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.servings,
                    onValueChange = viewModel::updateServings,
                    label = { Text("Servings") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = state.sourceUrl,
                onValueChange = viewModel::updateSourceUrl,
                label = { Text("Source URL") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
            )
        }
    }
}

@Composable
private fun CategorySelectionSection(
    selectedCategoryIds: Set<String>,
    allCategories: List<RecipeCategoryEntity>,
    onToggleCategory: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val tree = remember(allCategories) { CategoryTreeUtils.buildTree(allCategories) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { expanded = !expanded }.fillMaxWidth()
        ) {
            Text("Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (selectedCategoryIds.isNotEmpty()) {
                Text("${selectedCategoryIds.size} selected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
        }

        if (expanded) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    tree.forEach { node ->
                        CategorySelectionItem(node, 0, selectedCategoryIds, onToggleCategory)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySelectionItem(
    node: CategoryNode,
    depth: Int,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit
) {
    var isNodeExpanded by remember { mutableStateOf(false) }
    val isSelected = node.category.id in selectedIds

    Column(modifier = Modifier.padding(start = (depth * 12).dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp)
        ) {
            if (node.children.isNotEmpty()) {
                IconButton(onClick = { isNodeExpanded = !isNodeExpanded }, modifier = Modifier.size(24.dp)) {
                    Icon(if (isNodeExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight, null)
                }
            } else {
                Spacer(Modifier.size(24.dp))
            }
            
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle(node.category.id) },
                modifier = Modifier.scale(0.8f)
            )
            Text(
                text = node.category.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onToggle(node.category.id) }.weight(1f)
            )
        }
        if (isNodeExpanded) {
            node.children.forEach { child ->
                CategorySelectionItem(child, depth + 1, selectedIds, onToggle)
            }
        }
    }
}

@Composable
private fun TagManagementSection(
    selectedTagIds: Set<String>,
    allTags: List<TagEntity>,
    frequentTags: List<TagEntity>,
    onToggleTag: (String) -> Unit,
    onCreateTag: (String) -> Unit
) {
    var tagInput by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val filteredSuggestions by remember(tagInput, selectedTagIds, allTags) {
        derivedStateOf {
            if (tagInput.isBlank()) emptyList()
            else allTags.filter { 
                it.name.contains(tagInput, ignoreCase = true) && it.id !in selectedTagIds 
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedTagIds.forEach { id ->
                allTags.find { it.id == id }?.let { tag ->
                    InputChip(
                        selected = true,
                        onClick = { onToggleTag(id) },
                        label = { Text(tag.name) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(12.dp)) }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = expanded && filteredSuggestions.isNotEmpty(),
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = tagInput,
                onValueChange = { 
                    tagInput = it
                    expanded = it.isNotBlank()
                },
                label = { Text("Add tag...") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = {
                    if (tagInput.isNotBlank()) {
                        IconButton(onClick = {
                            if (allTags.none { it.name.equals(tagInput, true) }) {
                                onCreateTag(tagInput)
                            } else {
                                allTags.find { it.name.equals(tagInput, true) }?.let { onToggleTag(it.id) }
                            }
                            tagInput = ""
                            expanded = false
                        }) {
                            Icon(Icons.Default.Add, null)
                        }
                    }
                }
            )
            ExposedDropdownMenu(
                expanded = expanded && filteredSuggestions.isNotEmpty(),
                onDismissRequest = { expanded = false }
            ) {
                filteredSuggestions.forEach { tag ->
                    DropdownMenuItem(
                        text = { Text(tag.name) },
                        onClick = {
                            onToggleTag(tag.id)
                            tagInput = ""
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        val mealTags = listOf("Dinner", "Lunch", "Breakfast", "Snack", "Beverage", "Dessert")
        val typeTags = listOf("Main Dish", "Side Dish", "Protein", "Vegetarian", "Vegan", "Comfort Food", "Family Staple", "Fish and Seafood", "Meat", "Pasta", "Sweet")
        val otherTags = listOf("Prep Ahead", "Batch Cooking", "Quick", "Special Meal")

        TagRow("Meal", mealTags, frequentTags, selectedTagIds, onToggleTag, onCreateTag)
        TagRow("Type", typeTags, frequentTags, selectedTagIds, onToggleTag, onCreateTag)
        TagRow("Considerations", otherTags, frequentTags, selectedTagIds, onToggleTag, onCreateTag)
    }
}

@Composable
private fun TagRow(
    title: String,
    tagNames: List<String>,
    frequentTags: List<TagEntity>,
    selectedTagIds: Set<String>,
    onToggleTag: (String) -> Unit,
    onCreateTag: (String) -> Unit
) {
    val sortedTagNames = tagNames.sortedByDescending { name ->
        frequentTags.find { it.name.equals(name, true) }?.let { tag ->
            frequentTags.indexOf(tag).takeIf { it != -1 }?.let { 1000 - it } ?: 0
        } ?: 0
    }

    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedTagNames) { name ->
                val existingTag = frequentTags.find { it.name.equals(name, true) }
                val isSelected = existingTag?.let { it.id in selectedTagIds } ?: false
                
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (existingTag != null) {
                            onToggleTag(existingTag.id)
                        } else {
                            onCreateTag(name)
                        }
                    },
                    label = { Text(name) }
                )
            }
        }
    }
}
